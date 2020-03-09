package org.bonitasoft.casedetails;

import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;



public class CaseDetailsToolbox {

    private static Logger logger = Logger.getLogger(CaseDetailsToolbox.class.getName());
    public final static String CST_FORMAT_DATE_JSON = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

    /** utility class should privatise the constructor */
    private CaseDetailsToolbox() {}
    /**
     * decode a String
     * 
     * @param value
     * @param defaultValue
     * @return
     */

    public static String jsonToString(Object value, String defaultValue) {
        if (value == null)
            return defaultValue;
        try {
            return value.toString();
        } catch (Exception e) {
        }
        return defaultValue;
    }

    public static Boolean jsonToBoolean(Object value, Boolean defaultValue) {
        try {
            if (value == null || value.toString().length() == 0)
                return defaultValue;
            return Boolean.valueOf(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<String> jsonToListString(Object value) {
        /* if value is null, instanceof return false */
        if ( !(value instanceof List))
            return new ArrayList<>();
        ArrayList<String> result = new ArrayList<>();
        List<Object> listValue = (List<Object>) value;
        for (Object oneValue : listValue) {
            result.add(oneValue == null ? null : oneValue.toString());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<Long> jsonToListLong(Object value) {

        /* if value is null, instanceof return false */
        if ( !(value instanceof List))
            return new ArrayList<>();
        ArrayList<Long> result = new ArrayList<>();
        List<Object> listValue = (List<Object>) value;
        for (Object oneValue : listValue) {
            try {
                if (oneValue != null)
                    result.add( Long.valueOf(oneValue.toString()));
            } catch (Exception e) {
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> jsonToListMap(Object value) {

        /* if value is null, instanceof return false */
        if ( !(value instanceof List))
            return new ArrayList<>();
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        List<Object> listValue = (List<Object>) value;
        for (Object oneValue : listValue) {
            try {
                if (oneValue != null)
                    result.add((Map<String, Object>) oneValue);
            } catch (Exception e) {
            }
        }
        return result;
    }

    /**
     * decode a date
     * 
     * @param value
     * @return
     */
    public static Date jsonToDate(Object value) {
        logger.fine("ToDate[" + value + "] class:" + (value == null ? "null" : value.getClass().getName()));
        if (value instanceof Date)
            return (Date) value;
        if (value instanceof String) {
            SimpleDateFormat sdf = new SimpleDateFormat( CST_FORMAT_DATE_JSON );
            Date myDate;
            try {
                myDate = sdf.parse(value.toString());
                logger.fine("After Parsing ToDate[" + myDate + "]");

            } catch (ParseException e) {
                return null;
            }
            return myDate;
        }
        return null;
    }

    /**
     * decode an integer
     * 
     * @param value
     * @param defaultValue
     * @return
     */
    public static Long jsonToLong(Object value, Long defaultValue) {
        if (value == null || value.toString().length() == 0)
            return defaultValue;
        if (value instanceof Integer)
            return ((Integer) value).longValue();
        if (value instanceof Long)
            return ((Long) value);
        try {
            return Long.valueOf(value.toString());
        } catch (Exception e) {
        }
        return defaultValue;
    }


    /**
     * get the connection form the datasource : depends of the application
     * server (Tomcat or jboss), the datasource name is different
     * 
     * @return
     */
    public static Connection getConnection() {
        Context ctx = null;
        try {
            ctx = new InitialContext();
        } catch (Exception e) {
            logger.severe("Cant' get an InitialContext : can't access the datasource");
            return null;
        }

        DataSource ds = null;
        Connection con = null;
        try {
            ds = (DataSource) ctx.lookup("java:/comp/env/bonitaSequenceManagerDS");
            con = ds.getConnection();
            return con;
        } catch (Exception e) {
        }
        try {
            if (ds == null) {
                ds = (DataSource) ctx.lookup("java:jboss/datasources/bonitaSequenceManagerDS");
                con = ds.getConnection();
                return con;
            }
        } catch (Exception e) {
        } 
        return null;
    }
}
