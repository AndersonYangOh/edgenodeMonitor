package com.iiot.edgenode.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.iiot.dao.EdgeNodeDao;
import com.iiot.edgenode.entity.EdgeNode;

@Service
public class EdgeNodeMonitorService {

	@Autowired
	private EdgeNodeDao edgeNodeDao;
	
	@Transactional(propagation = Propagation.REQUIRED)
	public void insert(EdgeNode edgeNode) {
		edgeNodeDao.insert(edgeNode);
	}
	
	@Transactional(propagation = Propagation.REQUIRED)
	public void clearEdgeNodeTable() {
		edgeNodeDao.clearEdgeNodeTable();
	}
	
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public List<EdgeNode> getNodesList(){
		return edgeNodeDao.getNodesList();
	}
	
	public int getAppNumFromNodename(String nodename) {
		return edgeNodeDao.getAppNumFromNodename(nodename);
	}
}
