package com.sunrise.wiki.db;

import com.sunrise.wiki.common.Statics;
import com.sunrise.wiki.db.beans.*;
import com.sunrise.wiki.utils.Utils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;

public class DBHelper  {

    private File dbFile;
    private Connection connection;
    private Statement statement;
    private String dbFilePath;
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
        connection = getConnection(dbFilePath);
        statement = getStatement();
    }

    public static DBHelper get(){
        try {
            return new DBHelper(Statics.DB_FILE_PATH);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        }
    }

    /**
     * 获取数据库连接
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
    private <T> T getBeanByRaw(String sql,Class<T> theClass){
        if(!dbFile.exists()){
            return null;
        }
        try {
            ResultSet cursor = statement.executeQuery(sql);
            if(null==cursor){
                return null;
            }
            List<T> data = cursor2List(cursor, theClass);
            if(null==data||data.isEmpty()){
                return null;
            }else {
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
    private <T> List<T> getBeanListByRaw(String sql,Class<T> theClass){
        if(!dbFile.exists()){
            return null;
        }
        try{
            ResultSet cursor = statement.executeQuery(sql);
            return cursor2List(cursor,theClass);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        }finally {
            destroyed();
        }
    }

    /***
     * 获取查询语句的第一行第一列值
     * @param sql SQL语句
     * @return 第一行第一列值
     */
    private String getOne(String sql){
        if(!dbFile.exists()){
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
        }finally {
            destroyed();
        }
    }

    /***
     * 获取 int-string map
     * @param sql SQL语句
     * @return
     */
    private Map<Integer,String> getIntStringMap(String sql,String key,String value){
        if(!dbFile.exists()){
            return null;
        }
        try {
            ResultSet cursor = statement.executeQuery(sql);
            Map<Integer,String> result = new HashMap<>();
            while (cursor.next()){
                result.put(cursor.getInt(cursor.findColumn(key)),cursor.getString(cursor.findColumn(value)));
            }
            cursor.close();
            return result;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        }finally {
            destroyed();
        }
    }

    /************************* public field **************************/

    /***
     * 获取角色基础数据
     */
    public List<RawUnitBasic> getCharaBase(){
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
                .append("up.self_text")
                .append(",IFNULL(au.unit_name, ud.unit_name) 'actual_name'")
                .append(" FROM unit_data AS ud")
                .append(" LEFT JOIN unit_profile AS up ON ud.unit_id = up.unit_id")
                .append(" LEFT JOIN actual_unit_background AS au ON substr(ud.unit_id,1,4) = substr(au.unit_id,1,4)")
                .append(" WHERE ud.comment <> ''").toString();

        return getBeanListByRaw(sql,RawUnitBasic.class);
    }

    /***
     * 获取角色星级数据
     */
    public RawUnitRarity getUnitRarity(int unitId){
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM unit_rarity")
                .append(" WHERE unit_id=")
                .append(unitId)
                .append(" ORDER BY rarity DESC").toString();
        return getBeanByRaw(sql,RawUnitRarity.class);
    }

    /***
     * 获取角色星级数据
     */
    public List<RawUnitRarity> getUnitRarityList(int unitId){
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM unit_rarity")
                .append(" WHERE unit_id=")
                .append(unitId)
                .append(" ORDER BY rarity DESC").toString();
        return getBeanListByRaw(sql,RawUnitRarity.class);
    }

    /***
     * 获取角色剧情数据
     */
    public List<RawCharaStoryStatus> getCharaStoryStatus(int charaId){
        // 国服-> 排除还没有实装的角色剧情
        if("CN".equals(Statics.USER_LOC)){
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
            return getBeanListByRaw(sql1,RawCharaStoryStatus.class);
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
        return getBeanListByRaw(sql,RawCharaStoryStatus.class);
    }

    /***
     * 获取角色Rank汇总数据
     * @param unitId 角色id
     * @return
     */
    public List<RawPromotionStatus> getCharaPromotionStatus(int unitId){
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM unit_promotion_status")
                .append(" WHERE unit_id=")
                .append(unitId)
                .append(" ORDER BY promotion_level DESC").toString();
        return getBeanListByRaw(sql,RawPromotionStatus.class);
    }

    /***
     * 获取角色装备数据
     * @param unitId 角色id
     * @return
     */
    public List<RawUnitPromotion> getCharaPromotion(int unitId){
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM unit_promotion")
                .append(" WHERE unit_id=")
                .append(unitId)
                .append(" ORDER BY promotion_level DESC").toString();
        return getBeanListByRaw(sql,RawUnitPromotion.class);
    }

    /***
     * 获取装备数据
     * @param slots 装备ids
     * @return
     */
    public List<RawEquipmentData> getEquipments(ArrayList<Integer> slots){
        String sql = new StringBuilder()
                .append("SELECT a.*")
                .append(",b.max_equipment_enhance_level")
                .append(" FROM equipment_data a,")
                .append(" ( SELECT promotion_level, max( equipment_enhance_level ) max_equipment_enhance_level FROM equipment_enhance_data GROUP BY promotion_level ) b")
                .append(" WHERE a.promotion_level = b.promotion_level ")
                .append("AND a.equipment_id IN (")
                .append(Utils.splitIntegerWithComma(slots))
                .append(")").toString();
        return getBeanListByRaw(sql,RawEquipmentData.class);
    }

    /***
     * 获取所有装备数据
     */
    public List<RawEquipmentData> getEquipmentAll(){
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
        return getBeanListByRaw(sql,RawEquipmentData.class);
    }

    /***
     * 获取装备强化数据
     * @param slots 装备ids
     * @return
     */
    public List<RawEquipmentEnhanceData> getEquipmentEnhance(ArrayList<Integer> slots){
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM equipment_enhance_rate")
                .append(" WHERE equipment_id IN (")
                .append(Utils.splitIntegerWithComma(slots))
                .append(")").toString();
        return getBeanListByRaw(sql,RawEquipmentEnhanceData.class);
    }

    /***
     * 获取装备强化数据
     * @param id 装备ids
     * @return
     */
    public RawEquipmentEnhanceData getEquipmentEnhance(int equipmentId){
        String sql = new StringBuilder()
                .append("SELECT *")
                .append(" FROM equipment_enhance_rate")
                .append(" WHERE equipment_id =")
                .append(equipmentId).toString();
        return getBeanByRaw(sql,RawEquipmentEnhanceData.class);
    }
}
