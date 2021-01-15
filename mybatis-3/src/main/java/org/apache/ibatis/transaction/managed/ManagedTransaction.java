/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.transaction.managed;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;

/**
 * {@link org.apache.ibatis.transaction.Transaction} that lets the container manage the full lifecycle of the transaction.
 * Delays connection retrieval until getConnection() is called.
 * Ignores all commit or rollback requests.
 * By default, it closes the connection but can be configured not to do it.
 *
 * @see ManagedTransactionFactory
 */
/**
 * @author Clinton Begin
 */
  //让容器管理事务的全部生命周期
  //延迟恢复连接，在getConnection方法被调用的时候才恢复
  //忽略所有commit和rollback请求
  //默认情况下，会自动关闭连接，但是可以通过配置修改
public class ManagedTransaction implements Transaction {

  private static final Log log = LogFactory.getLog(ManagedTransaction.class);

  //数据源
  private DataSource dataSource;
  //事务的隔离级别
  private TransactionIsolationLevel level;
  //数据库连接
  private Connection connection;
  //是否关闭连接
  // 在通过ManagedTransactionFactory创建实例时若未赋值则会被赋值为true(ManagedTransactionFactory定义了该字段并默认为true)
  private boolean closeConnection;

  public ManagedTransaction(Connection connection, boolean closeConnection) {
    this.connection = connection;
    this.closeConnection = closeConnection;
  }

  public ManagedTransaction(DataSource ds, TransactionIsolationLevel level, boolean closeConnection) {
    this.dataSource = ds;
    this.level = level;
    this.closeConnection = closeConnection;
  }

  @Override
  public Connection getConnection() throws SQLException {
    if (this.connection == null) {
      openConnection();
    }
    return this.connection;
  }

  //提交，实际并未提交，因为交给容器管理
  @Override
  public void commit() throws SQLException {
    // Does nothing
  }

  //回滚，实际并未回滚，因为交给容器管理
  @Override
  public void rollback() throws SQLException {
    // Does nothing
  }

  //关闭当前所持有的数据库连接
  @Override
  public void close() throws SQLException {
    //若当前的属性closeConnection=true 且 当前持有的连接不为空
    if (this.closeConnection && this.connection != null) {
      if (log.isDebugEnabled()) {
        log.debug("Closing JDBC Connection [" + this.connection + "]");
      }
      //关闭连接
      this.connection.close();
    }
  }

  //打开连接
  protected void openConnection() throws SQLException {
    if (log.isDebugEnabled()) {
      log.debug("Opening JDBC Connection");
    }
    this.connection = this.dataSource.getConnection();
    //若属性-事务隔离级别 不为空
    if (this.level != null) {
      //设置该连接的事务隔离级别
      this.connection.setTransactionIsolation(this.level.getLevel());
    }
  }

}
