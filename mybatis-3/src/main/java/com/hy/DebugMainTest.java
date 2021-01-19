package com.hy;

import com.hy.VO.User;
import com.hy.mapper.UserMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * @Description:
 * @Author: xianchao.hua
 * @Create: 2021-01-04 11:46
 */
public class DebugMainTest {

    public static void main(String[] args) {
        //xml方式启动
        String resource = "mybatis-config.xml";
        InputStream inputStream = null;
        try {
            inputStream = Resources.getResourceAsStream(resource);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //获取SqlsessionFactory
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        //获取session
        SqlSession sqlSession = sqlSessionFactory.openSession();
        //获取mapper
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        UserMapper mapper2 = sqlSession.getMapper(UserMapper.class);
        System.out.println(mapper.equals(mapper2));
        User user = mapper.getById(1);
        System.out.println(user);
    }

}
