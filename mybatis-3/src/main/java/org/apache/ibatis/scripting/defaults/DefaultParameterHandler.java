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
package org.apache.ibatis.scripting.defaults;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeException;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class DefaultParameterHandler implements ParameterHandler {

  //类型处理器注册器，从MappedStatement中获取Configuration再获取该属性
  private final TypeHandlerRegistry typeHandlerRegistry;
  //要处理的MappedStatement
  private final MappedStatement mappedStatement;
  //参数对象
  private final Object parameterObject;
  //BoundSql
  private BoundSql boundSql;
  //环境配置,从MappedStatement属性中获取
  private Configuration configuration;

  //构造方法
  public DefaultParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
    this.mappedStatement = mappedStatement;
    this.configuration = mappedStatement.getConfiguration();
    this.typeHandlerRegistry = mappedStatement.getConfiguration().getTypeHandlerRegistry();
    this.parameterObject = parameterObject;
    this.boundSql = boundSql;
  }

  @Override
  public Object getParameterObject() {
    return parameterObject;
  }

  //对于预编译的SQL设置参数的方法
  @Override
  public void setParameters(PreparedStatement ps) {
    ErrorContext.instance().activity("setting parameters").object(mappedStatement.getParameterMap().getId());
    //1：获取BoundSql的参数映射集
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    //若获取的参数映射集不为空
    if (parameterMappings != null) {
      //1.1：遍历参数映射列表
      for (int i = 0; i < parameterMappings.size(); i++) {
        //取出每一个参数映射--这里实际就是#{}中的数据
        ParameterMapping parameterMapping = parameterMappings.get(i);
        //1.2：只有入参需要预编译设置参数,（PS：存储过程中有入参和出参）
        if (parameterMapping.getMode() != ParameterMode.OUT) {
          Object value;
          //1.3：取出参数名，比如说#{id} ，这里取出的就是id
          String propertyName = parameterMapping.getProperty();
          //boundSql是否涉及其它参数，这里基本不会走到
          if (boundSql.hasAdditionalParameter(propertyName)) { // issue #448 ask first for additional params
            value = boundSql.getAdditionalParameter(propertyName);
          } else if (parameterObject == null) {
            value = null;
          }
          //1.4：根据参数类型找到其对应的TypeHandler（类型处理器）
          //使用与参数类型为 非自定义类型 且Myabtis内置了该参数Class的类型处理器
          else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
            value = parameterObject;
          }

          //适用于自定义类型的参数传递，根据参数名使用反射拿到实际属性值
          else {
            MetaObject metaObject = configuration.newMetaObject(parameterObject);
            value = metaObject.getValue(propertyName);
          }

          //1.5：根据上面1.4找到类型处理器，进行类型转换，Java类型到Jdbc类型的转换
          //PS：自定义的参数类型 如 User 使用的 UnknownTypeHandler（在MapperXMl文件解析时就已经确定）
          TypeHandler typeHandler = parameterMapping.getTypeHandler();

          //获取参数对应的jdbcType(mapperxml中配置项)
          JdbcType jdbcType = parameterMapping.getJdbcType();
          if (value == null && jdbcType == null) {
            //若实际参数值为null 且 jdbcType为null 根据Configuration中的配置，设置jdbcType
            jdbcType = configuration.getJdbcTypeForNull();
          }
          try {
            //3：重点 调用对应的TypeHandler的方法，为prepareStatement设置正确的参数值
            typeHandler.setParameter(ps, i + 1, value, jdbcType);
          } catch (TypeException e) {
            throw new TypeException("Could not set parameters for mapping: " + parameterMapping + ". Cause: " + e, e);
          } catch (SQLException e) {
            throw new TypeException("Could not set parameters for mapping: " + parameterMapping + ". Cause: " + e, e);
          }
        }
      }
    }
  }

}
