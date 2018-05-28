# 校验是否需要设置管理员密码
## url
	man/pbcia.do
## response
	{
		"retCode": "1",  -- 1:需要设置  -1:不需要设置
		"retMsg": ""
	}
# 设置管理员密码
## url	
	man/pbraa.do
## request
	{
		"pwd": ""
	}
## response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": "",
		"token": "" -- 成功后返回
	}
# 管理员登录
## url
	man/pblgi.do
## request
	{
		"pwd": ""
	}
## response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": "",
		"token": "" -- 成功后返回
	}
# 查询创世块
## url
	man/pbggb.do
## response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": "",
		"hash": "" -- 成功后返回
	}
# 读取节点摘要信息
## url
	man/pbgni.do
## response 
	{}
# 批量读取配置信息
## url
	man/pbbqc.do
## request
	{
		"keys": ["", ""]
	}
## response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": "",
		"values": ["", ""]
	}
# 批量设置配置信息
## url
	man/pbbsc.do
## request
	{
		"keys": ["", ""],
		"values": ["", ""]
	}
## response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": ""
	}
# 设置net
## url
	man/pbsnw.do
## request
	{
		"network": "devnet_1"
	}
## response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": ""
	}
# 读取net
## url
	man/pbgnw.do
## response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": "",
		"network": ""
	}
# 创建节点账户
## url
	man/pbcna.do
## request
	{
		"pwd": ""
	}
## response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": ""
	}
# 导入节点账户
## url
	man/pbsna.do
## request
	{
		"keyStoreJsonStr": ""
		"pwd": ""
	}
## response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": ""
	}
# 导出节点账户
## url
	man/pbena.do
## request
	{
		"pwd": ""
	}
## response
	{
		"retCode": "", -- 1:成功  -1:失败
		"retMsg": ""
		"keyStoreJsonStr": ""
	} 
# 获取节点账户信息(推荐使用浏览器接口)
## url
	man/pbgna.do
## request
	{
		"address": ""
	}
## response
	{}