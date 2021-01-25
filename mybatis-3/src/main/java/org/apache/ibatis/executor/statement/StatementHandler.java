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

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.ResultHandler;

/**
 * @author Clinton Begin
 */
//Statement处理器,负责管理Statement的创建及与数据库的交互
public interface StatementHandler {

  //创建一个具体的Statement类型的实例(可以是Statement的子类，如PrepareStatement)
  //在SqlSession接收到指令操作时，由Configuration的newStatementHandler创建，
  //  意味着newStatementHandler是被Executor的查询，更新(增，删，改)触发的
  //  即StatementHandler是由Executor负责管理和创建的
  Statement prepare(Connection connection)
      throws SQLException;

  //初始化Statement实例,并对sql占位符赋值
  void parameterize(Statement statement)
      throws SQLException;

  void batch(Statement statement)
      throws SQLException;

  //通知Statement将insert、update、delete 操作推送到数据库
  int update(Statement statement)
      throws SQLException;

  //通知 Statement 将 select 操作推送数据库并返回对应的查询结果
  <E> List<E> query(Statement statement, ResultHandler resultHandler)
      throws SQLException;

  //获取BoundSql
  BoundSql getBoundSql();

  //获取参数处理器
  ParameterHandler getParameterHandler();

}
