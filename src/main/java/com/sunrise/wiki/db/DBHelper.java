package com.sunrise.wiki.db;

import com.sunrise.wiki.common.Statics;
import com.sunrise.wiki.db.beans.*;
import com.sunrise.wiki.utils.Utils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;

public class DBHelper {

    private File dbFile;
    private Connection connection;
    private Statement statement;
    private String dbFilePath;

    private static DBHelper dbHelper = null;
    /**
     * 构造函数
     *
     * @param dbFilePath sqlite db 文件路径
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public DBHelper(String dbFilePath) throws ClassNotFoundException, SQLException {
        this.dbFile = new File(dbFilePath);
        this.dbFilePath = dbFilePath;
        this.connection = getConnection(dbFilePath);
        this.statement = getStatement();
    }

    public static DBHelper get() {
        if(dbHelper==null){
            try {
                dbHelper = new DBHelper(Statics.DB_FILE_PATH);
                return dbHelper;
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
                return null;
            }
        }else {
            return dbHelper;
        }
    }

    /**
     * 获取数据库连接
     *
     * @param dbFilePath db文件路径
     * @return 数据库连接
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private Connection getConnection(String dbFilePath) throws ClassNotFoundException, SQLException {
        Connection conn = null;
        Class.forName("org.sqlite.JDBC");
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
        return conn;
    }

    private Connection getConnection() throws ClassNotFoundException, SQLException {
        if (null == connection) connection = getConnection(dbFilePath);
        return connection;
    }

    private Statement getStatement() throws SQLException, ClassNotFoundException {
        if (null == statement) statement = getConnection().createStatement();
        return statement;
    }

    /**
     * 数据库资源关闭和释放
     */
    private void destroyed() {
        try {
            if (null != connection) {
                connection.close();
                connection = null;
            }

            if (null != statement) {
                statement.close();
                statement = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private <T> List<T> cursor2List(ResultSet cursor, Class<T> theClass) {
        Field[] arrField = theClass.getFields();
        ArrayList<T> result = new ArrayList<>();
        try {
            while (cursor.next()) {
                if (cursor.isBeforeFirst()) {
                    continue;
                }
                T bean = theClass.getDeclaredConstructor().newInstance();
                for (Field f : arrField) {
                    String columnName = f.getName();
                    //特别注意，此方法理解并参考于Android中的Cursor.getColumnIndex()，不知是否正确
                    int columnIdx = cursor.findColumn(columnName);
                    if (columnIdx != -1) {
                        if (f.isAccessible()) {
                            f.setAccessible(true);
                        }
                        Class<?> type = f.getType();
                        if (type == Byte.TYPE) {
                            f.set(bean, cursor.getByte(columnIdx));
                        } else if (type == Short.TYPE) {
                            f.set(bean, cursor.getShort(columnIdx));
                        } else if (type == Integer.TYPE) {
                            f.set(bean, cursor.getInt(columnIdx));
                        } else if (type == Long.TYPE) {
                            f.set(bean, cursor.getLong(columnIdx));
                        } else if (type == String.class) {
                            f.set(bean, cursor.getString(columnIdx));
                        } else if (type == Blob.class) {
                            f.set(bean, cursor.getBlob(columnIdx));
                        } else if (type == Boolean.TYPE) {
                            f.set(bean, cursor.getInt(columnIdx) == 1);
                        } else if (type == Float.TYPE) {
                            f.set(bean, cursor.getFloat(columnIdx));
                        } else if (type == Double.TYPE) {
                            f.set(bean, cursor.getDouble(columnIdx));
                        }
                    }
                }
                result.add((T) bean);
            }
        } catch (SQLException | InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                cursor.close();
                destroyed();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        return result;
    }

    /***
     * 准备游标
     * @param tableName 表名
     * @param key WHERE [key] IN ([keyValue])
     * @param keyValue WHERE [key] IN ([keyValue])
     * @return 数据集
     */
    private ResultSet prepareCursor(String tableName, String key, List<String> keyValue) {
        try {
            if (!dbFile.exists()) {
                return null;
            }
            if (key == null || keyValue == null || keyValue.isEmpty()) {
                return statement.executeQuery(("SELECT * FROM" + tableName));
            } else {
                StringBuilder paraBuilder = new StringBuilder();
                paraBuilder.append("(");
                for (String s : keyValue) {
                    if (null != s && !"".equals(s)) {
                        paraBuilder.append("?");
                        paraBuilder.append(", ");
                    }
                }
                paraBuilder.delete(paraBuilder.length() - 2, paraBuilder.length());
                paraBuilder.append(")");
                PreparedStatement statement = getConnection().prepareStatement(
                        "SELECT * FROM" + tableName + "WHERE" + key + "IN" + paraBuilder.toString(),
                        keyValue.toArray(new String[keyValue.size()])
                );
                return statement.executeQuery();
            }
        } catch (SQLException | ClassNotFoundException throwables) {
            throwables.printStackTrace();
            return null;
        }
    }

    /******************* Method For Use  */
    /***
     * 由表名和类名无条件从数据库获取实体列表
     * @param tableName 表名
     * @param theClass 类名
     * @param <T> theClass的类
     * @return 生成的实体列表
    </T> */
    private <T> List<T> getBeanList(String tableName, Class<T> theClass) {
        ResultSet cursor = prepareCursor(tableName, null, null);
        if (null == cursor) {
            return null;
        } else {
            return cursor2List(cursor, theClass);
        }
    }

    /***
     * 由表名、类名、条件键值从数据库获取实体列表
     * @param tableName 表名
     * @param theClass 类名
     * @param key WHERE [key] IN ([keyValue])
     * @param keyValues WHERE [key] IN ([keyValue])
     * @param <T> theClass的类
     * @return 生成的实体列表
    </T> */
    private <T> List<T> getBeanList(String tableName, Class<T> theClass, String key, List<String> keyValues) {
        ResultSet cursor = prepareCursor(tableName, key, keyValues);
        if (null == cursor) {
            return null;
        } else {
            return cursor2List(cursor, theClass);
        }
    }

    /***
     * 由表名、类名、条件键值从数据库获取单个实体
     * @param tableName 表名
     * @param theClass 类名
     * @param key WHERE [key] IN ([keyValue])
     * @param keyValue WHERE [key] IN ([keyValue])
     * @param <T> theClass的类
     * @return 生成的实体
    </T> */
    private <T> T getBean(String tableName, Class<T> theClass, String key, String keyValue) {
        ResultSet cursor = prepareCursor(tableName, key, Collections.singletonList(keyValue));
        if (null == cursor) {
            return null;
        } else {
            List<T> data = cursor2List(cursor, theClass);
            if (null == data || data.isEmpty()) {
                return null;
            } else {
                return data.get(0);
            }
        }
    }

    /***
     * 由SQL语句从数据库获取单个实体
     * @param sql SQL语句
     * @param theClass 类名
     * @param <T> theClass的类
     * @return 生成的实体
    </T> */
    private <T> T getBeanByRaw(String sql, Class<T> theClass) {
        if (!dbFile.exists()) {
            return null;
        }
        try {
            ResultSet cursor = statement.executeQuery(sql);
            if (null == cursor) {
                return null;
            }
            List<T> data = cursor2List(cursor, theClass);
            if (null == data || data.isEmpty()) {
                return null;
            } else {
                return data.get(0);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        }
    }

    /***
     * 由SQL语句无条件从数据库获取实体列表
     * @param sql SQL语句
     * @param theClass 类名
     * @param <T> theClass的类
     * @return 生成的实体列表
    </T> */
    private <T> List<T> getBeanListByRaw(String sql, Class<T> theClass) {
        if (!dbFile.exists()) {
            return null;
        }
        try {
            ResultSet cursor = statement.executeQuery(sql);
            return cursor2List(cursor, theClass);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        } finally {
            destroyed();
        }
    }

    /***
     * 获取查询语句的第一行第一列值
     * @param sql SQL语句
     * @return 第一行第一列值
     */
    private String getOne(String sql) {
        if (!dbFile.exists()) {
            return null;
        }
        try {
            ResultSet cursor = statement.executeQuery(sql);
            cursor.next();
            String result = cursor.getString(0);
            cursor.close();
            return result;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        } finally {
            destroyed();
        }
    }

    /***
     * 获取 int-string map
     * @param sql SQL语句
     * @return
     */
    private Map<Integer, String> getIntStringMap(String sql, String key, String value) {
        if (!dbFile.exists()) {
            return null;
        }
        try {
            ResultSet cursor = statement.executeQuery(sql);
            Map<Integer, String> result = new HashMap<>();
            while (cursor.next()) {
                result.put(cursor.getInt(cursor.findColumn(key)), cursor.getString(cursor.findColumn(value)));
            }
            cursor.close();
            return result;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        } finally {
            destroyed();
        }
    }

    /************************* public field **************************/

    /***
     * 获取角色基础数据
     */
    public List<RawUnitBasic> getCharaBase() {
        StringBuilder sb = new StringBuilder();
        String sql = sb.append("SELECT ud.unit_id")
                .append(",ud.unit_name")
                .append(",ud.kana")
                .append(",ud.prefab_id")
                .append(",ud.move_speed")
                .append(",ud.search_area_width")
                .append(",ud.atk_type")
                .append(",ud.normal_atk_cast_time")
                .append(",ud.guild_id")
                .append(",ud.comment")
                .append(",ud.start_time")
                .append(",up.age")
                .append(",up.guild")
                .append(",up.race")
                .append(",up.height")
                .append(",up.weight")
                .append(",up.birth_month")
                .append(",up.birth_day")
                .append(",up.blood_type")
                .append(",up.favorite")
                .append(",up.voice")
                .append(",up.catch_copy")
                .append(",up.self_text")
                .append(",IFNULL(au.unit_name, ud.unit_name) 'actual_name'")
                .append(" FROM unit_data AS ud")
                .append(" LEFT JOIN unit_profile AS up ON ud.unit_id = up.unit_id")
                .append(" LEFT JOIN actual_unit_background AS au ON substr(ud.unit_id,1,4) = substr(au.unit_id,1,4)")
                .append(" WHERE ud.comment <> ''").toString();

        return getBeanListByRaw(sql, RawUnitBasic.class);
    }

    /**
     * 获取单个角色的基本信息
     * @param unitId 角色id
     * @return
     */
    public RawUnitBasic getCharaInfo(int unitId){
        String sql = new StringBuilder()
                .append("SELECT ud.unit_id")
                .append(",ud.unit_name")
                .append(",ud.kana")
                .append(",ud.prefab_id")
                .append(",ud.move_speed")
                .append(",ud.search_area_width")
                .append(",ud.atk_type")
                .append(",ud.normal_atk_cast_time")
                .append(",ud.guild_id")
                .append(",ud.comment")
                .append(",ud.start_time")
                .append(",up.age")
                .append(",up.guild")
                .append(",up.race")
                .append(",up.height")
                .append(",up.weight")
                .append(",up.birth_month")
                .append(",up.birth_day")
                .append(",up.blood_type")
                .append(",up.favorite")
                .append(",up.voice")
                .append(",up.catch_copy")
                .append(",up.self_text")
                .append(",IFNULL(au.unit_name, ud.unit_name) 'actual_name'")
                .append(" FROM unit_data AS ud")
                .append(" LEFT JOIN unit_profile AS up ON ud.unit_id = up.unit_id")
                .append(" LEFT JOIN actual_unit_background AS au ON substr(ud.unit_id,1,4) = substr(au.unit_id,1,4)")
                .append(" WHERE ud.unit_id=")
                .append(unitId).toString();
        return getBeanByRaw(sql,RawUnitBasic.class);
    }

    /***
     * 获取角色星级数据
     */
    public RawUnitRarity getUnitRarity(int unitId) {
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM unit_rarity")
                .append(" WHERE unit_id=")
                .append(unitId)
                .append(" ORDER BY rarity DESC").toString();
        return getBeanByRaw(sql, RawUnitRarity.class);
    }

    /***
     * 获取角色星级数据
     */
    public List<RawUnitRarity> getUnitRarityList(int unitId) {
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM unit_rarity")
                .append(" WHERE unit_id=")
                .append(unitId)
                .append(" ORDER BY rarity DESC").toString();
        return getBeanListByRaw(sql, RawUnitRarity.class);
    }

    /***
     * 获取角色剧情数据
     */
    public List<RawCharaStoryStatus> getCharaStoryStatus(int charaId) {
        // 国服-> 排除还没有实装的角色剧情
        if ("CN".equals(Statics.USER_LOC)) {
            String sql1 = new StringBuilder()
                    .append("SELECT a.*")
                    .append(" FROM chara_story_status AS a")
                    .append(" INNER JOIN unit_data AS b ON substr(a.story_id,1,4) = substr(b.unit_id,1,4)")
                    .append(" WHERE a.chara_id_1 = ")
                    .append(charaId)
                    .append(" OR a.chara_id_2 = ")
                    .append(charaId)
                    .append(" OR a.chara_id_3 = ")
                    .append(charaId)
                    .append(" OR a.chara_id_4 = ")
                    .append(charaId)
                    .append(" OR a.chara_id_5 = ")
                    .append(charaId)
                    .append(" OR a.chara_id_6 = ")
                    .append(charaId)
                    .append(" OR a.chara_id_7 = ")
                    .append(charaId)
                    .append(" OR a.chara_id_8 = ")
                    .append(charaId)
                    .append(" OR a.chara_id_9 = ")
                    .append(charaId)
                    .append(" OR a.chara_id_10 = ")
                    .append(charaId).toString();
            return getBeanListByRaw(sql1, RawCharaStoryStatus.class);
        }
        String sql = new StringBuilder()
                .append("SELECT a.*")
                .append(" FROM chara_story_status")
                .append(" WHERE a.chara_id_1 = ")
                .append(charaId)
                .append(" OR a.chara_id_2 = ")
                .append(charaId)
                .append(" OR a.chara_id_3 = ")
                .append(charaId)
                .append(" OR a.chara_id_4 = ")
                .append(charaId)
                .append(" OR a.chara_id_5 = ")
                .append(charaId)
                .append(" OR a.chara_id_6 = ")
                .append(charaId)
                .append(" OR a.chara_id_7 = ")
                .append(charaId)
                .append(" OR a.chara_id_8 = ")
                .append(charaId)
                .append(" OR a.chara_id_9 = ")
                .append(charaId)
                .append(" OR a.chara_id_10 = ")
                .append(charaId).toString();
        return getBeanListByRaw(sql, RawCharaStoryStatus.class);
    }

    /***
     * 获取角色Rank汇总数据
     * @param unitId 角色id
     * @return
     */
    public List<RawPromotionStatus> getCharaPromotionStatus(int unitId) {
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM unit_promotion_status")
                .append(" WHERE unit_id=")
                .append(unitId)
                .append(" ORDER BY promotion_level DESC").toString();
        return getBeanListByRaw(sql, RawPromotionStatus.class);
    }

    /***
     * 获取角色装备数据
     * @param unitId 角色id
     * @return
     */
    public List<RawUnitPromotion> getCharaPromotion(int unitId) {
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM unit_promotion")
                .append(" WHERE unit_id=")
                .append(unitId)
                .append(" ORDER BY promotion_level DESC").toString();
        return getBeanListByRaw(sql, RawUnitPromotion.class);
    }

    /***
     * 获取装备数据
     * @param slots 装备ids
     * @return
     */
    public List<RawEquipmentData> getEquipments(ArrayList<Integer> slots) {
        String sql = new StringBuilder()
                .append("SELECT a.*")
                .append(",b.max_equipment_enhance_level")
                .append(" FROM equipment_data a,")
                .append(" ( SELECT promotion_level, max( equipment_enhance_level ) max_equipment_enhance_level FROM equipment_enhance_data GROUP BY promotion_level ) b")
                .append(" WHERE a.promotion_level = b.promotion_level ")
                .append("AND a.equipment_id IN (")
                .append(Utils.splitIntegerWithComma(slots))
                .append(")").toString();
        return getBeanListByRaw(sql, RawEquipmentData.class);
    }

    /***
     * 获取所有装备数据
     */
    public List<RawEquipmentData> getEquipmentAll() {
        String sql = new StringBuilder()
                .append("SELECT a.*")
                .append(",ifnull(b.max_equipment_enhance_level, 0) 'max_equipment_enhance_level'")
                .append(",e.description 'catalog'")
                .append(",substr(a.equipment_id,3,1) * 10 + substr(a.equipment_id,6,1) 'rarity'")
                .append(",f.condition_equipment_id_1")
                .append(",f.consume_num_1")
                .append(",f.condition_equipment_id_2")
                .append(",f.consume_num_2")
                .append(",f.condition_equipment_id_3")
                .append(",f.consume_num_3")
                .append(",f.condition_equipment_id_4")
                .append(",f.consume_num_4")
                .append(",f.condition_equipment_id_5")
                .append(",f.consume_num_5")
                .append(",f.condition_equipment_id_6")
                .append(",f.consume_num_6")
                .append(",f.condition_equipment_id_7")
                .append(",f.consume_num_7")
                .append(",f.condition_equipment_id_8")
                .append(",f.consume_num_8")
                .append(",f.condition_equipment_id_9")
                .append(",f.consume_num_9")
                .append(",f.condition_equipment_id_10")
                .append(",f.consume_num_10")
                .append(" FROM equipment_data a")
                .append(" LEFT JOIN ( SELECT promotion_level, max( equipment_enhance_level ) max_equipment_enhance_level FROM equipment_enhance_data GROUP BY promotion_level ) b ON a.promotion_level = b.promotion_level")
                .append(" LEFT JOIN equipment_enhance_rate AS e ON a.equipment_id=e.equipment_id")
                .append(" LEFT JOIN equipment_craft AS f ON a.equipment_id = f.equipment_id")
                .append(" WHERE a.equipment_id < 113000")
                .append(" ORDER BY substr(a.equipment_id,3,1) * 10 + substr(a.equipment_id,6,1) DESC, a.require_level DESC, a.equipment_id ASC").toString();
        return getBeanListByRaw(sql, RawEquipmentData.class);
    }

    /***
     * 获取装备强化数据
     * @param slots 装备ids
     * @return
     */
    public List<RawEquipmentEnhanceData> getEquipmentEnhance(ArrayList<Integer> slots) {
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM equipment_enhance_rate")
                .append(" WHERE equipment_id IN (")
                .append(Utils.splitIntegerWithComma(slots))
                .append(")").toString();
        return getBeanListByRaw(sql, RawEquipmentEnhanceData.class);
    }

    /***
     * 获取装备强化数据
     * @param equipmentId 装备id
     * @return
     */
    public RawEquipmentEnhanceData getEquipmentEnhance(int equipmentId) {
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM equipment_enhance_rate")
                .append(" WHERE equipment_id =")
                .append(equipmentId).toString();
        return getBeanByRaw(sql, RawEquipmentEnhanceData.class);
    }

    /***
     * 获取所有装备强化数据
     */
    public List<RawEquipmentEnhanceData> getEquipmentEnhance() {
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM equipment_enhance_rate").toString();
        return getBeanListByRaw(sql, RawEquipmentEnhanceData.class);
    }

    /***
     * 获取专属装备数据
     * @param unitId 角色id
     * @return
     */
    public RawUniqueEquipmentData getUniqueEquipment(int unitId) {
        String sql = new StringBuilder()
                .append("SELECT e.*")
                .append(",c.item_id_1")
                .append(",c.consume_num_1")
                .append(",c.item_id_2")
                .append(",c.consume_num_2")
                .append(",c.item_id_3")
                .append(",c.consume_num_3")
                .append(",c.item_id_4")
                .append(",c.consume_num_4")
                .append(",c.item_id_5")
                .append(",c.consume_num_5")
                .append(",c.item_id_6")
                .append(",c.consume_num_6")
                .append(",c.item_id_7")
                .append(",c.consume_num_7")
                .append(",c.item_id_8")
                .append(",c.consume_num_8")
                .append(",c.item_id_9")
                .append(",c.consume_num_9")
                .append(",c.item_id_10")
                .append(",c.consume_num_10")
                .append(" FROM unique_equipment_data AS e")
                .append(" JOIN unit_unique_equip AS u ON e.equipment_id=u.equip_id")
                .append(" LEFT JOIN unique_equipment_craft AS c ON e.equipment_id=c.equip_id")
                .append(" WHERE u.unit_id=")
                .append(unitId).toString();
        return getBeanByRaw(sql, RawUniqueEquipmentData.class);
    }

    /***
     * 获取专属装备强化数据
     * @param unitId 角色id
     * @return
     */
    public RawUniqueEquipmentEnhanceData getUniqueEquipmentEnhance(int unitId) {
        String sql = new StringBuilder()
                .append("SELECT e.*")
                .append(" FROM unique_equipment_enhance_rate AS e")
                .append(" JOIN unit_unique_equip AS u ON e.equipment_id=u.equip_id")
                .append(" WHERE u.unit_id=")
                .append(unitId).toString();
        return getBeanByRaw(sql, RawUniqueEquipmentEnhanceData.class);
    }

    /***
     * 获取角色技能数据
     * @param unitId 角色id
     * @return
     */
    public RawUnitSkillData getUnitSkillData(int unitId) {
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM unit_skill_data")
                .append(" WHERE unit_id =")
                .append(unitId).toString();
        return getBeanByRaw(sql, RawUnitSkillData.class);
    }

    /***
     * 获取技能数据
     * @param skillId 技能id
     * @return
     */
    public RawSkillData getSkillData(int skillId) {
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM skill_data")
                .append(" WHERE skill_id=")
                .append(skillId).toString();
        return getBeanByRaw(sql, RawSkillData.class);
    }

    /***
     * 获取技能动作数据
     * @param actionId 动作id
     * @return
     */
    public RawSkillAction getSkillAction(int actionId) {
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM skill_action")
                .append(" WHERE action_id=")
                .append(actionId).toString();
        return getBeanByRaw(sql, RawSkillAction.class);
    }

    /***
     * 获取行动顺序
     * @param unitId 角色id
     * @return
     */
    public List<RawUnitAttackPattern> getUnitAttackPattern(int unitId) {
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM unit_attack_pattern")
                .append(" WHERE unit_id=")
                .append(unitId)
                .append(" ORDER BY pattern_id ").toString();
        return getBeanListByRaw(sql, RawUnitAttackPattern.class);
    }

    /***
     * 获取会战期次
     * @param
     * @return
     */
    public List<RawClanBattlePeriod> getClanBattlePeriod() {
        // 国服-> 读取所有记录
        if ("CN".equals(Statics.USER_LOC)) {
            String sql1 = new StringBuilder()
                    .append("SELECT *")
                    .append(" FROM clan_battle_period")
                    .append(" ORDER BY clan_battle_id DESC").toString();
            return getBeanListByRaw(sql1, RawClanBattlePeriod.class);
        }
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM clan_battle_period")
                .append(" WHERE clan_battle_id > 1014")
                .append(" ORDER BY clan_battle_id DESC").toString();
        return getBeanListByRaw(sql, RawClanBattlePeriod.class);
    }

    /***
     * 获取会战phase
     * @param
     * @return
     */
    public List<RawClanBattlePhase> getClanBattlePhase(int clanBattleId) {
        // 国服-> 迎合日服结构
        if ("CN".equals(Statics.USER_LOC)) {
            String sql1 = new StringBuilder()
                    .append("SELECT a.difficulty 'phase'")
                    .append(",b1.wave_group_id 'wave_group_id_1'")
                    .append(",b2.wave_group_id 'wave_group_id_2'")
                    .append(",b3.wave_group_id 'wave_group_id_3'")
                    .append(",b4.wave_group_id 'wave_group_id_4'")
                    .append(",b5.wave_group_id 'wave_group_id_5'")
                    .append(" FROM clan_battle_map_data AS a")
                    .append(" JOIN clan_battle_boss_group AS b1 ON a.clan_battle_boss_group_id = b1.clan_battle_boss_group_id AND b1.order_num = 1")
                    .append(" JOIN clan_battle_boss_group AS b2 ON a.clan_battle_boss_group_id = b2.clan_battle_boss_group_id AND b2.order_num = 2")
                    .append(" JOIN clan_battle_boss_group AS b3 ON a.clan_battle_boss_group_id = b3.clan_battle_boss_group_id AND b3.order_num = 3")
                    .append(" JOIN clan_battle_boss_group AS b4 ON a.clan_battle_boss_group_id = b4.clan_battle_boss_group_id AND b4.order_num = 4")
                    .append(" JOIN clan_battle_boss_group AS b5 ON a.clan_battle_boss_group_id = b5.clan_battle_boss_group_id AND b5.order_num = 5")
                    .append(" WHERE a.clan_battle_id=")
                    .append(clanBattleId)
                    .append(" AND a.lap_num_from <> a.lap_num_to")
                    .append(" ORDER BY a.difficulty DESC").toString();
            return getBeanListByRaw(sql1, RawClanBattlePhase.class);
        }
        String sql = new StringBuilder()
                .append("SELECT DISTINCT")
                .append(" phase")
                .append(",wave_group_id_1")
                .append(",wave_group_id_2")
                .append(",wave_group_id_3")
                .append(",wave_group_id_4")
                .append(",wave_group_id_5")
                .append(" FROM clan_battle_2_map_data WHERE clan_battle_id=")
                .append(clanBattleId)
                .append(" ORDER BY phase DESC").toString();
        return getBeanListByRaw(sql, RawClanBattlePhase.class);
    }

    /***
     * 获取wave列表
     * @param
     * @return
     */
    public List<RawWaveGroup> getWaveGroupData(List<Integer> waveGroupList) {
        String sql = String.format(new StringBuilder()
                .append(" SELECT *")
                .append(" FROM wave_group_data")
                .append(" WHERE wave_group_id IN ( %s ) ")
                .toString(), waveGroupList.toString().replace("[", "").replace("]", ""));
        return getBeanListByRaw(sql, RawWaveGroup.class);
    }

    /**
     * 获取wave
     *
     * @param waveGroupId
     * @return
     */
    public RawWaveGroup getWaveGroupData(int waveGroupId) {
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM wave_group_data")
                .append(" WHERE wave_group_id = ")
                .append(waveGroupId).toString();
        return getBeanByRaw(sql, RawWaveGroup.class);
    }

    /**
     * 获取enemyList
     *
     * @param enemyIdList
     * @return
     */
    public List<RawEnemy> getEnemy(List<Integer> enemyIdList) {
        if ("CN".equals(Statics.USER_LOC)) {
            String sql1 = String.format(new StringBuilder()
                    .append("SELECT a.*")
                    .append(",b.union_burst")
                    .append(",b.union_burst_evolution")
                    .append(",b.main_skill_1")
                    .append(",b.main_skill_evolution_1")
                    .append(",b.main_skill_2")
                    .append(",b.main_skill_evolution_2")
                    .append(",b.ex_skill_1")
                    .append(",b.ex_skill_evolution_1")
                    .append(",b.main_skill_3")
                    .append(",b.main_skill_4")
                    .append(",b.main_skill_5")
                    .append(",b.main_skill_6")
                    .append(",b.main_skill_7")
                    .append(",b.main_skill_8")
                    .append(",b.main_skill_9")
                    .append(",b.main_skill_10")
                    .append(",b.ex_skill_2")
                    .append(",b.ex_skill_evolution_2")
                    .append(",b.ex_skill_3 ")
                    .append(",b.ex_skill_evolution_3 ")
                    .append(",b.ex_skill_4 ")
                    .append(",b.ex_skill_evolution_4 ")
                    .append(",b.ex_skill_5 ")
                    .append(",b.sp_skill_1 ")
                    .append(",b.ex_skill_evolution_5 ")
                    .append(",b.sp_skill_2 ")
                    .append(",b.sp_skill_3 ")
                    .append(",b.sp_skill_4 ")
                    .append(",b.sp_skill_5 ")
                    .append(",u.prefab_id ")
                    .append(",u.atk_type ")
                    .append(",u.normal_atk_cast_time")
                    .append(",u.search_area_width")
                    .append(",u.comment")
                    .append("FROM")
                    .append("unit_skill_data b")
                    .append(",enemy_parameter a ")
                    .append("LEFT JOIN unit_enemy_data u ON a.unit_id = u.unit_id ")
                    .append("WHERE ")
                    .append("a.unit_id = b.unit_id ")
                    .append("AND a.enemy_id in ( %s ) ")
                    .toString(), enemyIdList.toString().replace("[", "").replace("]", ""));
            return getBeanListByRaw(sql1, RawEnemy.class);
        }
        String sql = String.format(new StringBuilder()
                .append("SELECT a.*")
                .append(",b.union_burst")
                .append(",b.union_burst_evolution")
                .append(",b.main_skill_1")
                .append(",b.main_skill_evolution_1")
                .append(",b.main_skill_2")
                .append(",b.main_skill_evolution_2")
                .append(",b.ex_skill_1")
                .append(",b.ex_skill_evolution_1")
                .append(",b.main_skill_3")
                .append(",b.main_skill_4")
                .append(",b.main_skill_5")
                .append(",b.main_skill_6")
                .append(",b.main_skill_7")
                .append(",b.main_skill_8")
                .append(",b.main_skill_9")
                .append(",b.main_skill_10")
                .append(",b.ex_skill_2")
                .append(",b.ex_skill_evolution_2")
                .append(",b.ex_skill_3 ")
                .append(",b.ex_skill_evolution_3 ")
                .append(",b.ex_skill_4 ")
                .append(",b.ex_skill_evolution_4 ")
                .append(",b.ex_skill_5 ")
                .append(",b.sp_skill_1 ")
                .append(",b.ex_skill_evolution_5 ")
                .append(",b.sp_skill_2 ")
                .append(",b.sp_skill_3 ")
                .append(",b.sp_skill_4 ")
                .append(",b.sp_skill_5 ")
                .append(",c.child_enemy_parameter_1 ")
                .append(",c.child_enemy_parameter_2 ")
                .append(",c.child_enemy_parameter_3 ")
                .append(",c.child_enemy_parameter_4 ")
                .append(",c.child_enemy_parameter_5 ")
                .append(",u.prefab_id ")
                .append(",u.atk_type ")
                .append(",u.normal_atk_cast_time")
                .append(",u.search_area_width")
                .append(",u.comment")
                .append("FROM")
                .append("unit_skill_data b ")
                .append(",enemy_parameter a ")
                .append("LEFT JOIN enemy_m_parts c ON a.enemy_id = c.enemy_id ")
                .append("LEFT JOIN unit_enemy_data u ON a.unit_id = u.unit_id ")
                .append("WHERE ")
                .append("a.unit_id = b.unit_id ")
                .append("AND a.enemy_id in ( %s ) ")
                .toString(), enemyIdList.toString().replace("[", "").replace("]", ""));
        return getBeanListByRaw(sql, RawEnemy.class);
    }

    /***
     * 获取第一个enemy
     * @param
     * @return
     */
    public RawEnemy getEnemy(int enemyId) {
        return getEnemy(Collections.singletonList(enemyId)).get(0);
    }

    /**
     * 获取敌人抗性值
     *
     * @param resistStatusId 抗性id
     * @return
     */
    public RawResistData getResistData(int resistStatusId) {
        String sql = new StringBuilder()
                .append("SELECT * ")
                .append("FROM resist_data ")
                .append("WHERE resist_status_id=")
                .append(resistStatusId).toString();
        return getBeanByRaw(sql, RawResistData.class);
    }

    /***
     * 获取友方召唤物
     */
    public RawUnitMinion getUnitMinion(int minionId) {
        String sql = new StringBuilder()
                .append("SELECT a.*,")
                .append("b.union_burst,")
                .append("b.union_burst_evolution,")
                .append("b.main_skill_1,")
                .append("b.main_skill_evolution_1,")
                .append("b.main_skill_2,")
                .append("b.main_skill_evolution_2,")
                .append("b.ex_skill_1,")
                .append("b.ex_skill_evolution_1,")
                .append("b.main_skill_3,")
                .append("b.main_skill_4,")
                .append("b.main_skill_5,")
                .append("b.main_skill_6,")
                .append("b.main_skill_7,")
                .append("b.main_skill_8,")
                .append("b.main_skill_9,")
                .append("b.main_skill_10,")
                .append("b.ex_skill_2,")
                .append("b.ex_skill_evolution_2,")
                .append("b.ex_skill_3,")
                .append("b.ex_skill_evolution_3,")
                .append("b.ex_skill_4,")
                .append("b.ex_skill_evolution_4,")
                .append("b.ex_skill_5,")
                .append("b.ex_skill_evolution_5,")
                .append("b.sp_skill_1,")
                .append("b.sp_skill_2,")
                .append("b.sp_skill_3,")
                .append("b.sp_skill_4,")
                .append("b.sp_skill_5,")
                .append("FROM ")
                .append("unit_skill_data b,")
                .append("unit_data a ")
                .append("WHERE ")
                .append("a.unit_id = b.unit_id ")
                .append("AND a.unit_id = ")
                .append(minionId).toString();
        return getBeanByRaw(sql, RawUnitMinion.class);
    }

    /***
     * 获取敌方召唤物
     */
    public RawEnemy getEnemyMinion(int enemyId) {
        String sql = new StringBuilder()
                .append("SELECT ")
                .append("d.unit_name,")
                .append("d.prefab_id,")
                .append("d.search_area_width,")
                .append("d.atk_type,")
                .append("d.move_speed,")
                .append("a.*,")
                .append("b.*,")
                .append("d.normal_atk_cast_time,")
                .append("c.child_enemy_parameter_1,")
                .append("c.child_enemy_parameter_2,")
                .append("c.child_enemy_parameter_3,")
                .append("c.child_enemy_parameter_4,")
                .append("c.child_enemy_parameter_5,")
                .append("FROM ")
                .append("enemy_parameter a ")
                .append("JOIN unit_skill_data AS b ON a.unit_id = b.unit_id ")
                .append("JOIN unit_enemy_data AS d ON a.unit_id = d.unit_id ")
                .append("LEFT JOIN enemy_m_parts c ON a.enemy_id = c.enemy_id ")
                .append("WHERE a.enemy_id = ")
                .append(enemyId).toString();
        return getBeanByRaw(sql, RawEnemy.class);
    }

    /**
     * 获取地城bossList
     *
     * @return
     */
    public List<RawDungeon> getDungeons() {
        String sql = new StringBuilder()
                .append("SELECT ")
                .append("a.dungeon_area_id,")
                .append("a.dungeon_name,")
                .append("a.description,")
                .append("b.*")
                .append("FROM ")
                .append("dungeon_area_data AS a ")
                .append("JOIN wave_group_data AS b ON a.wave_group_id=b.wave_group_id ")
                .append("ORDER BY a.dungeon_area_id DESC ").toString();
        return getBeanListByRaw(sql, RawDungeon.class);
    }

    /***
     * 获取所有Quest
     */
    public List<RawQuest> getQuests() {
        String sql = "SELECT * FROM quest_data WHERE quest_id < 13000000 ORDER BY daily_limit ASC, quest_id DESC ";
        return getBeanListByRaw(sql, RawQuest.class);
    }

    /***
     * 获取掉落奖励
     */
    public List<RawEnemyRewardData> getEnemyRewardData(List<Integer> dropRewardIdList) {
        String sql = String.format("SELECT * FROM enemy_reward_data WHERE drop_reward_id IN ( %s ) ", dropRewardIdList.toString().replace("[", "").replace("]", ""));
        return getBeanListByRaw(sql, RawEnemyRewardData.class);
    }

    /***
     * 获取campaign日程
     */
    public List<RawScheduleCampaign> getCampaignSchedule(String nowTimeString) {
        String sqlString = " SELECT * FROM campaign_schedule ";
        if (null != nowTimeString) {
            sqlString += "WHERE end_time > " + nowTimeString;
        }
        return getBeanListByRaw(sqlString, RawScheduleCampaign.class);
    }

    /***
     * 获取free gacha日程
     */
    public List<RawScheduleFreeGacha> getFreeGachaSchedule(String nowTimeString) {
        String sqlString = " SELECT * FROM campaign_freegacha ";
        if (null != nowTimeString) {
            sqlString += "WHERE end_time > " + nowTimeString;
        }
        return getBeanListByRaw(sqlString, RawScheduleFreeGacha.class);
    }

    /***
     * 获取hatsune日程
     */
    public List<RawScheduleHatsune> getHatsuneSchedule(String nowTimeString) {
        String sqlString = "SELECT a.event_id, a.start_time, a.end_time, b.title FROM hatsune_schedule AS a JOIN event_story_data AS b ON a.event_id = b.value";
        if (null != nowTimeString) {
            sqlString += "WHERE end_time > " + nowTimeString;
        }
        sqlString += " ORDER BY a.event_id DESC ";
        return getBeanListByRaw(sqlString, RawScheduleHatsune.class);
    }

    /***
     * 获取hatsune一般boss数据
     */
    public List<RawHatsuneBoss> getHatsuneBattle(int eventId) {
        String sql = new StringBuilder()
                .append("SELECT ")
                .append("a.*")
                .append("FROM ")
                .append("hatsune_boss a ")
                .append("WHERE ")
                .append("event_id = ")
                .append(eventId)
                .append("AND area_id <> 0").toString();
        return getBeanListByRaw(sql, RawHatsuneBoss.class);
    }

    /***
     * 获取hatsune SP boss数据
     */
    public List<RawHatsuneSpecialBattle> getHatsuneSP(int eventId) {
        String sql = new StringBuilder()
                .append("SELECT ")
                .append("a.* ")
                .append("FROM hatsune_special_battle a ")
                .append("WHERE event_id = ")
                .append(eventId).toString();
        return getBeanListByRaw(sql, RawHatsuneSpecialBattle.class);
    }

    /***
     * 获取露娜塔日程
     */
    public List<RawTowerSchedule> getTowerSchedule(String nowTimeString) {
        String sqlString = " SELECT * FROM tower_schedule ";
        if (null != nowTimeString) {
            sqlString += " WHERE end_time > " + nowTimeString;
        }
        return getBeanListByRaw(sqlString, RawTowerSchedule.class);
    }

    /***
     * 获取装备碎片
     */
    public List<RawEquipmentPiece> getEquipmentPiece() {
        return getBeanListByRaw(" SELECT * FROM equipment_data WHERE equipment_id >= 113000 ",
                RawEquipmentPiece.class
        );
    }

    public Map<Integer, String> ailmentMap;
    public Integer maxCharaLevel;
    public Integer maxCharaRank;
    public Integer maxUniqueEquipmentLevel;
    public Integer maxEnemyLevel;
    public String randomId;

    public Map<Integer, String> getAilmentMap() {
        ailmentMap = getIntStringMap("SELECT * FROM ailment_data ", "ailment_id", "ailment_name");
        return ailmentMap;
    }

    public Integer getMaxCharaLevel() {
        String result = getOne("SELECT max(team_level) FROM experience_team ");
        try {
            maxCharaLevel = Integer.parseInt(result);
        } catch (Exception e) {
            e.printStackTrace();
            maxCharaLevel = 0;
        }
        return maxCharaLevel;
    }

    public Integer getMaxCharaRank() {
        String result = getOne("SELECT max(promotion_level) FROM unit_promotion ");
        try{
            maxCharaRank = Integer.parseInt(result);
        }catch (Exception e){
            e.printStackTrace();
            maxCharaRank = 0;
        }
        return maxCharaRank;
    }

    public Integer getMaxUniqueEquipmentLevel() {
        String result = getOne("SELECT max(enhance_level) FROM unique_equipment_enhance_data ");
        try{
            maxUniqueEquipmentLevel = Integer.parseInt(result);
        }catch (Exception e){
            e.printStackTrace();
            maxUniqueEquipmentLevel = 0;
        }
        return maxUniqueEquipmentLevel;
    }

    public Integer getMaxEnemyLevel() {
        String result = getOne("SELECT MAX(level) FROM enemy_parameter ");
        try{
            maxEnemyLevel = Integer.parseInt(result);
        }catch (Exception e){
            e.printStackTrace();
            maxEnemyLevel = 0;
        }
        return maxEnemyLevel;
    }

    public String getRandomId() {
        String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        int[] nums = new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
        for(int i : nums){
            int number = random.nextInt(36);
            sb.append(str.charAt(number));
        }
        randomId = sb.toString();
        return randomId;
    }
}
