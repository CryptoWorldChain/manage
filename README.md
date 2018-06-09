# 节点管理器 后台接口说明

## 节点管理器介绍
	cwv区块链中的每个节点都会对应一个节点管理器，用于管理该节点中的信息，包括：网络信息、配置信息、账户信息等
	
	1. 新节点在初次打开区块链浏览器时需要先校验是否已经[存在管理账户](http://localhost:8000/fbs/man/pbcia.do)，
	2. 已经存在则直接需要直接输入密[登录](http://localhost:8000/fbs/man/pblgi.do)码进行管理页面
	3. 不存在则需要[设置管理员密码](http://localhost:8000/fbs/man/pbraa.do)
	4. [设置网络](http://localhost:8000/fbs/man/pbsnw.do)，可以选择该节点的网络配置
	5. [获取节点摘要信息](http://localhost:8000/fbs/man/pbgni.do)
	6.  

## 校验是否需要设置管理员密码
### url
	man/pbcia.do
### response
	{
		"retCode": "1",  -- 1:需要设置  -1:不需要设置
		"retMsg": ""
	}
## 设置管理员密码
#### url	
	man/pbraa.do
#### request
	{
		"pwd": ""
	}
#### response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": "",
		"token": "" -- 成功后返回
	}
## 管理员登录
#### url
	man/pblgi.do
#### request
	{
		"pwd": ""
	}
#### response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": "",
		"token": "" -- 成功后返回
	}
## 查询创世块
#### url
	man/pbggb.do
#### response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": "",
		"hash": "" -- 成功后返回
	}
## 读取节点摘要信息
#### url
	man/pbgni.do
#### response 
	{}
## 批量读取配置信息
#### url
	man/pbbqc.do
#### request
	{
		"keys": ["", ""]
	}
#### response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": "",
		"values": ["", ""]
	}
## 批量设置配置信息
#### url
	man/pbbsc.do
#### request
	{
		"keys": ["", ""],
		"values": ["", ""]
	}
#### response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": ""
	}
## 设置net
#### url
	man/pbsnw.do
#### request
	{
		"network": "devnet_1"
	}
#### response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": ""
	}
## 读取net
#### url
	man/pbgnw.do
#### response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": "",
		"network": ""
	}
## 创建节点账户
#### url
	man/pbcna.do
#### request
	{
		"pwd": ""
	}
#### response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": ""
	}
## 导入节点账户
#### url
	man/pbsna.do
#### request
	{
		"keyStoreJsonStr": ""
		"pwd": ""
	}
#### response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": ""
	}
## 导出节点账户
#### url
	man/pbena.do
#### request
	{
		"pwd": ""
	}
#### response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": ""
		"keyStoreJsonStr": ""
	} 
## 获取节点账户信息(推荐使用浏览器接口)
#### url
	man/pbgna.do
#### request
	{
		"address": ""
	}
#### response
	{}