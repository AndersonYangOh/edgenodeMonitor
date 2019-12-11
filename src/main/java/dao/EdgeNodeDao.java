package com.iiot.dao;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.type.JdbcType;

import com.iiot.edgenode.entity.EdgeNode;
import com.iiot.mqtt.client.MQTTPersist;

public interface EdgeNodeDao {
	
	@Insert({"insert into iiot_edge_node(hostname, ip, run_status, device_bind_status, app_deploy_num) "
			+ "values(#{hostName}, #{ip}, #{runStatus}, #{deviceBindStatus}, #{appNum})"})
	int insert(EdgeNode edgeNode);
	
	@Update({"truncate table iiot_edge_node"})
	void clearEdgeNodeTable();
	
	@Select({"select * from iiot_edge_node"})
	@Results({
		@Result(column="hostname", property="hostName", jdbcType=JdbcType.VARCHAR),
		@Result(column="ip", property="ip", jdbcType=JdbcType.VARCHAR),
		@Result(column="run_status", property="runStatus", jdbcType=JdbcType.VARCHAR),
		@Result(column="device_bind_status", property="deviceBindStatus", jdbcType=JdbcType.VARCHAR),
		@Result(column="app_deploy_num", property="appNum", jdbcType=JdbcType.INTEGER)
	})
	List<EdgeNode> getNodesList();
	
	@Select({"SELECT COUNT(1) FROM iiot_app_list WHERE STATUS = 1 AND node = #{nodename}"})
	int getAppNumFromNodename(String nodename);
	
	@Insert({"insert into iiot_mqtt_data(host, topic, data) values(#{host}, #{topic}, #{data})"})
	int insertMQTT(MQTTPersist mqttPersist);
	
	@Delete({"delete from iiot_mqtt_data where host = #{host} and topic = #{topic}"})
	int deleteMQTT(MQTTPersist mqttPersist);
	
	@Select({"select * from iiot_mqtt_data where host = #{host} and topic = #{topic}"})
	@Results({
		@Result(column="host", property="host", jdbcType=JdbcType.VARCHAR),
		@Result(column="topic", property="topic", jdbcType=JdbcType.VARCHAR),
		@Result(column="data", property="data", jdbcType=JdbcType.LONGVARCHAR)
	})
	List<MQTTPersist> selectMQTT(MQTTPersist mqttPersist);
}
