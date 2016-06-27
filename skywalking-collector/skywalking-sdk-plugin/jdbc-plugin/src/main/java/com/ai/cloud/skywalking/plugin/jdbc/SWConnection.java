package com.ai.cloud.skywalking.plugin.jdbc;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class SWConnection implements java.sql.Connection {
    private String connectInfo;

    private final java.sql.Connection realConnection;

    public SWConnection(String url, Properties info,
                        java.sql.Connection realConnection) {
        super();
        this.connectInfo = url + "(" + info.getProperty("user") + ")";
        this.realConnection = realConnection;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return realConnection.unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return realConnection.isWrapperFor(iface);
    }

    public Statement createStatement() throws SQLException {
        return new SWStatement(this, realConnection.createStatement(),
                this.connectInfo);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return new SWPreparedStatement(this,
                realConnection.prepareStatement(sql), this.connectInfo, sql);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        return new SWCallableStatement(this, realConnection.prepareCall(sql),
                this.connectInfo, sql);
    }

    public String nativeSQL(String sql) throws SQLException {
        return realConnection.nativeSQL(sql);
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        realConnection.setAutoCommit(autoCommit);
    }

    public boolean getAutoCommit() throws SQLException {
        return realConnection.getAutoCommit();
    }

    public void commit() throws SQLException {
        ConnectionTracing.execute(realConnection, connectInfo, "commit", "",
                new ConnectionTracing.Executable<String>() {
                    public String exe(java.sql.Connection realConnection,
                                      String sql) throws SQLException {
                        realConnection.commit();
                        return null;
                    }
                });
    }

    public void rollback() throws SQLException {
        ConnectionTracing.execute(realConnection, connectInfo, "rollback", "",
                new ConnectionTracing.Executable<String>() {
                    public String exe(java.sql.Connection realConnection,
                                      String sql) throws SQLException {
                        realConnection.rollback();
                        return null;
                    }
                });
    }

    public void close() throws SQLException {
        ConnectionTracing.execute(realConnection, connectInfo, "close", "",
                new ConnectionTracing.Executable<String>() {
                    public String exe(java.sql.Connection realConnection,
                                      String sql) throws SQLException {
                        realConnection.close();
                        return null;
                    }
                });
    }

    public boolean isClosed() throws SQLException {
        return realConnection.isClosed();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return realConnection.getMetaData();
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        realConnection.setReadOnly(readOnly);
    }

    public boolean isReadOnly() throws SQLException {
        return realConnection.isReadOnly();
    }

    public void setCatalog(String catalog) throws SQLException {
        realConnection.setCatalog(catalog);
    }

    public String getCatalog() throws SQLException {
        return realConnection.getCatalog();
    }

    public void setTransactionIsolation(int level) throws SQLException {
        realConnection.setTransactionIsolation(level);
    }

    public int getTransactionIsolation() throws SQLException {
        return realConnection.getTransactionIsolation();
    }

    public SQLWarning getWarnings() throws SQLException {
        return realConnection.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        realConnection.clearWarnings();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return new SWStatement(this, realConnection.createStatement(
                resultSetType, resultSetConcurrency), this.connectInfo);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency) throws SQLException {
        return new SWPreparedStatement(this, realConnection.prepareStatement(
                sql, resultSetType, resultSetConcurrency), this.connectInfo,
                sql);
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
                                         int resultSetConcurrency) throws SQLException {
        return new SWCallableStatement(this, realConnection.prepareCall(sql,
                resultSetType, resultSetConcurrency), this.connectInfo, sql);
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return realConnection.getTypeMap();
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        realConnection.setTypeMap(map);
    }

    public void setHoldability(int holdability) throws SQLException {
        realConnection.setHoldability(holdability);
    }

    public int getHoldability() throws SQLException {
        return realConnection.getHoldability();
    }

    public Savepoint setSavepoint() throws SQLException {
        return realConnection.setSavepoint();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        return realConnection.setSavepoint(name);
    }

    public void rollback(final Savepoint savepoint) throws SQLException {
        ConnectionTracing.execute(realConnection, connectInfo,
                "rollback to savepoint", "", new ConnectionTracing.Executable<String>() {
                    public String exe(java.sql.Connection realConnection,
                                      String sql) throws SQLException {
                        realConnection.rollback(savepoint);
                        return null;
                    }
                });
    }

    public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
        ConnectionTracing.execute(realConnection, connectInfo,
                "releaseSavepoint savepoint", "", new ConnectionTracing.Executable<String>() {
                    public String exe(java.sql.Connection realConnection,
                                      String sql) throws SQLException {
                        realConnection.releaseSavepoint(savepoint);
                        return null;
                    }
                });
    }

    public Statement createStatement(int resultSetType,
                                     int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return new SWStatement(this, realConnection.createStatement(
                resultSetType, resultSetConcurrency, resultSetHoldability),
                this.connectInfo);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return new SWPreparedStatement(this,
                realConnection.prepareStatement(sql, resultSetType,
                        resultSetConcurrency, resultSetHoldability),
                this.connectInfo, sql);
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
                                         int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return new SWCallableStatement(this, realConnection.prepareCall(sql,
                resultSetType, resultSetConcurrency, resultSetHoldability), this.connectInfo, sql);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        return new SWPreparedStatement(this, realConnection.prepareStatement(
                sql, autoGeneratedKeys), this.connectInfo, sql);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
            throws SQLException {
        return new SWPreparedStatement(this, realConnection.prepareStatement(
                sql, columnIndexes), this.connectInfo, sql);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        return new SWPreparedStatement(this, realConnection.prepareStatement(
                sql, columnNames), this.connectInfo, sql);
    }

    public Clob createClob() throws SQLException {
        return realConnection.createClob();
    }

    public Blob createBlob() throws SQLException {
        return realConnection.createBlob();
    }

    public NClob createNClob() throws SQLException {
        return realConnection.createNClob();
    }

    public SQLXML createSQLXML() throws SQLException {
        return realConnection.createSQLXML();
    }

    public boolean isValid(int timeout) throws SQLException {
        return realConnection.isValid(timeout);
    }

    public void setClientInfo(String name, String value)
            throws SQLClientInfoException {
        realConnection.setClientInfo(name, value);
    }

    public void setClientInfo(Properties properties)
            throws SQLClientInfoException {
        realConnection.setClientInfo(properties);
    }

    public String getClientInfo(String name) throws SQLException {
        return realConnection.getClientInfo(name);
    }

    public Properties getClientInfo() throws SQLException {
        return realConnection.getClientInfo();
    }

    public Array createArrayOf(String typeName, Object[] elements)
            throws SQLException {
        return realConnection.createArrayOf(typeName, elements);
    }

    public Struct createStruct(String typeName, Object[] attributes)
            throws SQLException {
        return realConnection.createStruct(typeName, attributes);
    }

    public void setSchema(String schema) throws SQLException {
        realConnection.setSchema(schema);
    }

    public String getSchema() throws SQLException {
        return realConnection.getSchema();
    }

    public void abort(Executor executor) throws SQLException {
        realConnection.abort(executor);
    }

    public void setNetworkTimeout(Executor executor, int milliseconds)
            throws SQLException {
        realConnection.setNetworkTimeout(executor, milliseconds);
    }

    public int getNetworkTimeout() throws SQLException {
        return realConnection.getNetworkTimeout();
    }

}
