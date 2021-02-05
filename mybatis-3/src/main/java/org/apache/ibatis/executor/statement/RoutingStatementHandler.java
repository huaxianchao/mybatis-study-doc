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
package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * @author Clinton Begin
 */
//工厂模式，
//这里也是个装饰模式，但是不同于Executor中的二级缓存的装饰模式，
// 这里并没有对方法做加强，只是提供了 按照类型创建对应的StatementHandler(子类)实例的逻辑
public class RoutingStatementHandler implements StatementHandler {

  private final StatementHandler delegate;

  //根据类型创建装饰的Statement类型
  public RoutingStatementHandler(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {

    switch (ms.getStatementType()) {
      case STATEMENT:
        delegate = new SimpleStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        break;
      case PREPARED:
        delegate = new PreparedStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        break;
      case CALLABLE:
        delegate = new CallableStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        break;
      default:
        throw new ExecutorException("Unknown statement type: " + ms.getStatementType());
    }

  }

  //实际调用的是 Statement/PrepareStatement/CallableStatement的prepare，装饰模式
  @Override
  public Statement prepare(Connection connection) throws SQLException {
    return delegate.prepare(connection);
  }

  //实际调用的是Statement/PrepapreStatement/CallableStatement的prepare方法，装饰模式
  @Override
  public void parameterize(Statement statement) throws SQLException {
    delegate.parameterize(statement);
  }

  //实际调用的是Statement/PrepapreStatement/CallableStatement的batch方法，装饰模式
  @Override
  public void batch(Statement statement) throws SQLException {
    delegate.batch(statement);
  }

  //实际调用的是Statement/PrepapreStatement/CallableStatement的update方法，装饰模式
  @Override
  public int update(Statement statement) throws SQLException {
    return delegate.update(statement);
  }

  @Override
  public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
    return delegate.<E>query(statement, resultHandler);
  }

  @Override
  public BoundSql getBoundSql() {
    return delegate.getBoundSql();
  }

  @Override
  public ParameterHandler getParameterHandler() {
    return delegate.getParameterHandler();
  }
}
