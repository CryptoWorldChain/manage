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