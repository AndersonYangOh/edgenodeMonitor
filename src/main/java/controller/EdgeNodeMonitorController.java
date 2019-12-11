package com.iiot.edgenode.controller;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttributes;

import com.alibaba.fastjson.JSONObject;
import com.iiot.edgenode.entity.EdgeNode;
import com.iiot.edgenode.entity.Message;
import com.iiot.edgenode.entity.MonitorRes;
import com.iiot.edgenode.service.EdgeNodeMonitorService;
import com.iiot.service.k8s.K8APIService;
import com.iiot.utils.HttpClientUtils;

import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeAddress;
import io.fabric8.kubernetes.api.model.NodeCondition;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.ku8.docker.registry.HTTPCallResult;

@RestController
@RequestMapping("/nodeMonitor")
@SessionAttributes(org.ku8eye.Constants.USER_SESSION_KEY)
public class EdgeNodeMonitorController {

	@Autowired
	private EdgeNodeMonitorService edgeNodeMonitorService;
	@Autowired
	private K8APIService k8APIService;
	@Value("${iiot.kubernetes.masterURL}")
	private String masterURL;
	@Value("${iiot.kubernetes.token}")
	private String OauthToken;
	
	
	@Value("${iiot.prometheus.url}")
	private String promeURL;
	
	@RequestMapping("/refresh")
	public Message refresh(HttpServletRequest request) {
		Message message = new Message();
		
		KubernetesClient client = k8APIService.getClient(1);
		try {
		    //NamespaceList myNs = client.namespaces().list();
		    NodeList nodeList = client.nodes().list();
		    //System.out.println("NameSpaceList is "+myNs.toString());
		    List<Node> items = nodeList.getItems();
		    System.out.println("totally "+items.size()+" nodes");
		    List<EdgeNode> list = new ArrayList<>();
		    edgeNodeMonitorService.clearEdgeNodeTable();
		    for(Node node: items) {
		    	EdgeNode edgeNode = new EdgeNode();
		    	List<NodeAddress> nodeAddresses = node.getStatus().getAddresses();
		    	for(NodeAddress address: nodeAddresses) {
		    		if(address.getType().equals("Hostname")) {
			    		edgeNode.setHostName(address.getAddress());
			    		edgeNode.setAppNum(edgeNodeMonitorService.getAppNumFromNodename(address.getAddress()));
		    		}else if(address.getType().equals("InternalIP")) {
		    			edgeNode.setIp(address.getAddress());
		    		}
		    	}
		    	List<NodeCondition> nodeConditions = node.getStatus().getConditions();
		    	for(NodeCondition condition: nodeConditions) {
		    		if(condition.getType().equals("Ready")) {
		    			edgeNode.setRunStatus(condition.getStatus().equals("False")?"Not Ready":"Ready");
		    		}
		    	}
		    	list.add(edgeNode);
		    	edgeNodeMonitorService.insert(edgeNode);
		    }
		    message.setFlag(true);
		    message.setMsg("success");
		    message.setResult(list);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			message.setFlag(false);
			message.setMsg("出錯啦！");
		}finally {
		    client.close();
		}
		return message;
	}
	
	@RequestMapping("/getNodesList")
	public Message getNodesListFromDB(HttpServletRequest request) {
		Message message = new Message();
		try {
			List<EdgeNode> list = edgeNodeMonitorService.getNodesList();
			message.setFlag(true);
		    message.setMsg("success");
		    message.setResult(list);		
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			message.setFlag(false);
			message.setMsg("出錯啦！");
		}
		return message;
	}
	
	@RequestMapping("/nodeCPUDetail")
	public Message nodeCPUDetail(HttpServletRequest request) {
		Message message = new Message();
		try {
			long current = System.currentTimeMillis();
			//毫秒为单位，从现在要查询到多久之前 5min前即300000ms
			long pregap = Long.valueOf(request.getParameter("pregap"));
			String ip = request.getParameter("ip");
			
			String query = URLEncoder.encode("100-(avg by (instance) (irate(node_cpu{mode=\"idle\", instance=\""+ip+":9100\"}[10m]))*100)","utf-8");
			//传入的时间戳单位是s step为30s
			String url = promeURL+"/api/v1/query_range?query="+query+"&start="+(current-pregap)/1000+"&end="+current/1000+"&step=30";
			HTTPCallResult res = new HttpClientUtils().httpGet(url, null, 3000);
			JSONObject jsonObject = JSONObject.parseObject(res.getContent());
			int size = jsonObject.getJSONObject("data").getJSONArray("result").getJSONObject(0).getJSONArray("values").size();
			String tmp = "";
			List<MonitorRes> mrlist = new ArrayList<>();
			SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
			for(int i=0; i<size; i++) {
				MonitorRes mRes = new MonitorRes();
				tmp = jsonObject.getJSONObject("data").getJSONArray("result").getJSONObject(0).getJSONArray("values").getJSONArray(i).toString();
				String[] ret = tmp.substring(1, tmp.length()-1).split(",");
				mRes.setTime(formatter.format(Long.valueOf(ret[0])*1000));
				mRes.setRes(String.format("%.2f", Double.valueOf(ret[1].substring(1, ret[1].length()-1))));
				mrlist.add(mRes);
			}
			message.setFlag(true);
		    message.setMsg("success");
		    message.setResult(mrlist);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			message.setFlag(false);
			message.setMsg("出錯啦！");
		}
		return message;
	}
	
	@RequestMapping("/nodeMemoryDetail")
	public Message nodeMemoryDetail(HttpServletRequest request) {
		Message message = new Message();
		Map<String,List<MonitorRes>> resultMap = new HashMap<>();
		try {
			long current = System.currentTimeMillis();
			//毫秒为单位，从现在要查询到多久之前 5min前即300000ms
			long pregap = Long.valueOf(request.getParameter("pregap"));
			String ip = request.getParameter("ip");
			
			String query_used = URLEncoder.encode("node_memory_MemTotal{instance=\""+ip+":9100\"} - node_memory_MemFree{instance=\""+ip+":9100\"} - node_memory_Buffers{instance=\""+ip+":9100\"} - node_memory_Cached{instance=\""+ip+":9100\"}","utf-8");
			String query_total = URLEncoder.encode("node_memory_MemTotal{instance=\""+ip+":9100\"}","utf-8");
			//传入的时间戳单位是s step为30s
			String url_used = promeURL+"/api/v1/query_range?query="+query_used+"&start="+(current-pregap)/1000+"&end="+current/1000+"&step=30";
			String url_total = promeURL+"/api/v1/query_range?query="+query_total+"&start="+(current-pregap)/1000+"&end="+current/1000+"&step=30";
			HTTPCallResult res_used = new HttpClientUtils().httpGet(url_used, null, 3000);
			HTTPCallResult res_total = new HttpClientUtils().httpGet(url_total, null, 3000);
			
			JSONObject jsonObject_used = JSONObject.parseObject(res_used.getContent());
			JSONObject jsonObject_total = JSONObject.parseObject(res_total.getContent());
			int size = jsonObject_used.getJSONObject("data").getJSONArray("result").getJSONObject(0).getJSONArray("values").size();
			String tmp = "";
			List<MonitorRes> mrlist_used = new ArrayList<>();
			List<MonitorRes> mrlist_total = new ArrayList<>();
			SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
			for(int i=0; i<size; i++) {
				//used
				MonitorRes mrUsed = new MonitorRes();
				tmp = jsonObject_used.getJSONObject("data").getJSONArray("result").getJSONObject(0).getJSONArray("values").getJSONArray(i).toString();
				String[] ret1 = tmp.substring(1, tmp.length()-1).split(",");
				mrUsed.setTime(formatter.format(Long.valueOf(ret1[0])*1000));
				mrUsed.setRes(String.format("%.2f", Double.valueOf(ret1[1].substring(1, ret1[1].length()-1))/1024/1024/1024));
				mrlist_used.add(mrUsed);
				
				//total
				MonitorRes mrTotal = new MonitorRes();
				tmp = jsonObject_total.getJSONObject("data").getJSONArray("result").getJSONObject(0).getJSONArray("values").getJSONArray(i).toString();
				String[] ret2 = tmp.substring(1, tmp.length()-1).split(",");
				mrTotal.setTime(formatter.format(Long.valueOf(ret2[0])*1000));
				mrTotal.setRes(String.format("%.2f", Double.valueOf(ret2[1].substring(1, ret2[1].length()-1))/1024/1024/1024));
				mrlist_total.add(mrTotal);
			}
			resultMap.put("used", mrlist_used);
			resultMap.put("total", mrlist_total);
			message.setFlag(true);
		    message.setMsg("success");
		    message.setResult(resultMap);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			message.setFlag(false);
			message.setMsg("出錯啦！");
		}
		return message;
	}
	
	@RequestMapping("/nodeDiskDetail")
	public Message nodeDiskDetail(HttpServletRequest request) {
		Message message = new Message();
		try {
			String ip = request.getParameter("ip");
			String query = URLEncoder.encode("(sum(node_filesystem_size{device!=\"rootfs\",instance=\""+ip+":9100\"}) - sum(node_filesystem_free{device!=\"rootfs\",instance=\""+ip+":9100\"})) / sum(node_filesystem_size{device!=\"rootfs\",instance=\""+ip+":9100\"})*100","utf-8");
			String url = promeURL+"/api/v1/query?query="+query;
			HTTPCallResult res = new HttpClientUtils().httpGet(url, null, 3000);
			JSONObject jsonObject = JSONObject.parseObject(res.getContent());
			String tmp = jsonObject.getJSONObject("data").getJSONArray("result").getJSONObject(0).getJSONArray("value").toString();
			String[] ret = tmp.substring(1, tmp.length()-1).split(",");
			String mRes = String.format("%.2f", Double.valueOf(ret[1].substring(1, ret[1].length()-1)));
			
			message.setFlag(true);
		    message.setMsg("success");
		    message.setResult(mRes);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			message.setFlag(false);
			message.setMsg("出錯啦！");
		}
		return message;
	}
	
}
