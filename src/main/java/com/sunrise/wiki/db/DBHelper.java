package com.sunrise.wiki.db;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SqliteHelper {

    private String dbFilePath;

    /**
     * 构造函数
     *
     * @param dbFilePath sqlite db 文件路径
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public DBHelper(String dbFilePath) throws ClassNotFoundException, SQLException {
        super(dbFilePath);
        this.dbFilePath = dbFilePath;
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
                    int columnIdx = cursor.getRow();
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
        }
        return result;
    }

    /***
     * 准备游标
     * @param tableName 表名
     * @param key WHERE [key] IN ([keyValue])
     * @param keyValue WHERE [key] IN ([keyValue])
     * @return 存有数据的游标
     */
    private ResultSet prepareCursor(String tableName, String key, List<String> keyValue) throws SQLException, ClassNotFoundException {
        File dbFile = new File(dbFilePath);
        if (!dbFile.exists()) {
            return null;
        }
        if (key == null || keyValue == null || keyValue.isEmpty()) {
//            db.rawQuery("SELECT * FROM $tableName ", null)
            return getStatement().executeQuery(("SELECT * FROM" + tableName));
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
    }
}
