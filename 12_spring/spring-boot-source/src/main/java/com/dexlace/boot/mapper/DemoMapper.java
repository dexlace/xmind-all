//package com.dexlace.boot.mapper;
//
//
//import com.dexlace.boot.bean.Demo;
//import com.dexlace.boot.bean.DemoExample;
//import org.jboss.logging.Param;
//
//import java.util.List;
//
//public interface DemoMapper {
//    long countByExample(DemoExample example);
//
//    int deleteByExample(DemoExample example);
//
//    int deleteByPrimaryKey(Long id);
//
//    int insert(Demo record);
//
//    int insertSelective(Demo record);
//
//    List<Demo> selectByExample(DemoExample example);
//
//    Demo selectByPrimaryKey(Long id);
//
//    int updateByExampleSelective(@Param("record") Demo record, @Param("example") DemoExample example);
//
//    int updateByExample(@Param("record") Demo record, @Param("example") DemoExample example);
//
//    int updateByPrimaryKeySelective(Demo record);
//
//    int updateByPrimaryKey(Demo record);
//}