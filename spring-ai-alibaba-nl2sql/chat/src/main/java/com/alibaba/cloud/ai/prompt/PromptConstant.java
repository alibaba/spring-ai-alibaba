package com.alibaba.cloud.ai.prompt;

import org.springframework.ai.chat.prompt.PromptTemplate;

public class PromptConstant {

	public static final String INIT_REWRITE_WITH_AND_EVIDENCE_CLARIFY_PROMPT_STRING = "你是一名数据分析专家，需要从用户和助理的对话中确定用户最新需求的需求类型和需求内容，你需要遵循：\n"
			+ "1. 判断用户的需求类型请从【需求类型】中进行选择。\n" + "2. 需求内容的生成，请严格按照语种类型生成，如果语种类型为《中文》，则以中文生成需求内容，如果语种类型为《英文》，则以英文生成需求内容。\n"
			+ "3. 需求内容的生成，需要进行必要的继承上文内容替换改写，特别是对<最新>中出现的指代（如这、这个、那等）进行名词替换；注意不要遗漏相关信息，但不要改变用户问题的原意，不要过度的解释说明，不需要再重复输出用户的历史需求。\n"
			+ "4. 当用户<最新>输入是回答助理的问题时，如果基于用户的回答已经能够明确意图，可以直接生成需求内容。当需要澄清时，需求内容为需要向用户反问的问题.\n" + "\n" + "【需求类型】\n"
			+ "《数据分析》：用户需求与【数据库】中的内容较为相关，能够通过查询数据库或对查询结果进行数据分析来满足用户需求\n"
			+ "《需要澄清》: 用户需求与【数据库】中的内容较为相关，但是所提供的信息不足以完成查询任务，需要用户提供更多信息\n" + "《自由闲聊》：开放域的自由聊天和不涉及【数据库】中相关内容的数据查询分析\n"
			+ "\n" + "【语种类型】\n" + "《中文》\n" + "《英文》\n" + "\n" + "下面是一些参考样例：\n" + "=======\n" + "【数据库】\n"
			+ "库名: pets_1, 包含以下表:\n" + "# 表名: 学生, 包含字段:\n" + "[\n" + "  (学生ID),\n" + "  (姓氏),\n" + "  (名字),\n"
			+ "  (年龄),\n" + "  (性别),\n" + "  (专业),\n" + "  (导师),\n" + "  (城市代码)\n" + "]\n" + "# 表名: 养宠物, 包含字段:\n"
			+ "[\n" + "  (学生ID),\n" + "  (宠物ID)\n" + "]\n" + "# 表名: 宠物, 包含字段:\n" + "[\n" + "  (宠物ID),\n" + "  (宠物类型),\n"
			+ "  (宠物年龄),\n" + "  (体重)\n" + "]\n" + "\n" + "【多轮输入】\n" + ">用户：小李的小鸡有多重啊\n" + ">用户：这个宠物多大了？\n"
			+ "<最新>用户：I like this one so much, haha.\n" + "\n" + "【输出】\n" + "需求类型：《自由闲聊》\n" + "语种类型：《英文》\n"
			+ "需求内容：I like this little chicken pet.\n" + "\n" + "=======\n" + "\n" + "【数据库】\n"
			+ "库名: 广州博奥有限公司工资, 主要为公司工资相关的信息库, 包含以下表:\n" + "# 表名: 事业群, 包含字段:\n" + "[\n" + "  (事业群编号),\n"
			+ "  (事业群名, 示例值:['基础平台', '游戏', '商业联盟', '文娱', '智能产品']),\n"
			+ "  (负责人, 示例值:['张晓阳', '曾南', '李一乐', '张艺', '马可']),\n" + "  (员工数量),\n" + "  (目标营收),\n" + "  (成本支出)\n" + "]\n"
			+ "# 表名: 部门人员, 包含字段:\n" + "[\n" + "  (部门编号),\n" + "  (年份),\n" + "  (员工数量),\n" + "  (离职人数),\n" + "  (招聘人数)\n"
			+ "]\n" + "# 表名: 部门, 包含字段:\n" + "[\n" + "  (部门编号),\n"
			+ "  (部门名称, 示例值:['搜索策略部门', '点击预估部门', '用户产品调研部', '基础架构部', '广告品牌部']),\n"
			+ "  (职责, 示例值:['机器管理及部署', '负责广告分发及维护', '广告排序', '用户目标群体调研及产品设计', '搜索基础策略研发及优化']),\n" + "  (所属群编号)\n" + "]\n"
			+ "# 表名: 员工, 包含字段:\n" + "[\n" + "  (员工编号),\n" + "  (姓名, 示例值:['陈晓程', '江夏', '张天天', '田小雨', '李乐乐']),\n"
			+ "  (职称, 示例值:['高级工程师', '架构工程师', '资深工程师', '工程师']),\n" + "  (薪资),\n" + "  (年龄),\n" + "  (工龄),\n"
			+ "  (所属部门id)\n" + "]\n" + "\n" + "【多轮输入】\n" + ">用户：按工资降序对员工进行排列。\n" + ">用户：哪个工资最高？\n" + ">用户：最低的呢？\n"
			+ ">用户：他们分别来自哪个部门？\n" + "<最新>用户：这些部门的职责\n" + "\n" + "【输出】\n" + "需求类型：《数据分析》\n" + "语种类型：《中文》\n"
			+ "需求内容：工资最高的和工资最低的员工的所属部门的职责分别是什么？\n" + "\n" + "=======\n" + "\n" + "【数据库】\n"
			+ "库名: world_1, World Information, 包含以下表:\n" + "# Table: city, 包含字段:\n" + "[\n" + "  (city id),\n"
			+ "  (name, Value examples: ['New York', 'paris', 'Beijing']),\n" + "  (country code),\n"
			+ "  (district),\n" + "  (population)\n" + "]\n" + "# Table: sequence, 包含字段:\n" + "[\n" + "  (名称),\n"
			+ "  (序列)\n" + "]\n" + "# Table: country, 包含字段:\n" + "[\n" + "  (Code),\n" + "  (Name),\n"
			+ "  (Continent),\n" + "  (Region),\n" + "  (Area),\n" + "  (Independence Year),\n" + "  (Population),\n"
			+ "  (Life Expectancy),\n" + "  (GDP, 国民生产总值.),\n" + "  (Old GDP),\n" + "  (Local Name),\n"
			+ "  (Government Form),\n" + "  (Leader),\n" + "  (Capital),\n" + "  (Code2)\n" + "]\n"
			+ "# Table: country_language, 包含字段:\n" + "[\n" + "  (country_code),\n" + "  (语言),\n" + "  (is_official),\n"
			+ "  (percentage),\n" + "  (is_MM)\n" + "]\n" + "\n" + "【多轮输入】\n" + ">用户：List all MM languages.\n"
			+ "<最新>用户：最古老的是哪个？\n" + "\n" + "【输出】\n" + "需求类型：《自由闲聊》\n" + "语种类型：《中文》\n" + "需求内容：最古老的MM语言是哪个？\n" + "\n"
			+ "=======\n" + "\n" + "【数据库】\n" + "库名: 网易云阅读, 包含以下表:\n" + "# 表名: 作者, 包含字段:\n" + "[\n" + "  (作者id),\n"
			+ "  (姓名, 示例值:['梁夜白', '唐七公子', '吱吱', '匪我思存', '林绾绾']),\n" + "  (年龄)\n" + "]\n" + "# 表名: 出版图书, 包含字段:\n" + "[\n"
			+ "  (出版图书id),\n" + "  (书名, 示例值:['激荡三十年', '贫穷的本质', '人类简史', '三体', '熊镇']),\n" + "  (作者id),\n" + "  (评分),\n"
			+ "  (评价人数),\n" + "  (字数),\n" + "  (点击数),\n" + "  (类型, 示例值:['人文社科', '经济管理', '小说', '科技书籍'])\n" + "]\n"
			+ "# 表名: 网络小说, 包含字段:\n" + "[\n" + "  (网络图书id),\n"
			+ "  (书名, 示例值:['三生三世十里桃花', '爱情的开关', '重生之锦绣皇后', '医妃权倾天下', '蓝桥几顾']),\n" + "  (作者id),\n" + "  (评分),\n"
			+ "  (类型, 示例值:['穿越', '种田', '同人', '古言', '现言']),\n" + "  (状态, 示例值:['完结', '更新中']),\n" + "  (价格)\n" + "]\n"
			+ "# 表名: 畅销榜, 包含字段:\n" + "[\n" + "  (网络小说id),\n" + "  (周排名),\n" + "  (月排名),\n" + "  (总排名),\n" + "  (省份)\n"
			+ "]\n" + "# 表名: 收藏榜, 包含字段:\n" + "[\n" + "  (网络小说id),\n" + "  (周排名),\n" + "  (月排名),\n" + "  (总排名)\n" + "]\n"
			+ "\n" + "【多轮输入】\n" + ">用户：列出已经更新完毕的网络小说有哪些？\n" + ">用户：列出销量最好的省份\n" + "<最新>用户：这个省哪个书今年三月份排名最高\n" + "\n"
			+ "【输出】\n" + "需求类型：《数据分析》\n" + "语种类型：《中文》\n" + "需求内容：销量最好的省份哪个书今年三月份排名最高\n" + "\n" + "=======\n" + "\n"
			+ "【数据库】\n" + "库名: 快递公司, 包含以下表:\n" + "# 表名: 快递公司, 包含字段:\n" + "[\n" + "  (公司id),\n"
			+ "  (公司名, Value examples: ['顺丰', '韵达', '圆通', '中通'].),\n" + "  (总部地点, Value examples: ['深圳', '杭州'].),\n"
			+ "  (成立时间, Value examples: ['2010年', '1999年'].),\n" + "  (员工数量),\n" + "  (运输车辆数),\n" + "  (覆盖城市数量),\n"
			+ "  (网点数量)\n" + "]\n" + "# 表名: 省份, 包含字段:\n" + "[\n" + "  (省id),\n"
			+ "  (省名, Value examples: ['浙江', '江西', '上海', '北京'].),\n"
			+ "  (所属区域, Value examples: ['长三角', '东北', '西北'].)\n" + "]\n" + "# 表名: 快递费, 包含字段:\n" + "[\n"
			+ "  (快递公司id),\n" + "  (区域),\n" + "  (起步价格),\n" + "  (起步公斤数),\n" + "  (每公斤价格)\n" + "]\n"
			+ "# 表名: 站点表, 包含字段:\n" + "[\n" + "  (驿站id),\n" + "  (收件总数),\n" + "  (驿站状态。0：正常，1：关闭),\n" + "  (驿站名字),\n"
			+ "  (驿站类型。0 自营。1 加盟。3 注资),\n" + "  (所属公司，Value examples: ['顺丰', '申通', '韵达'])\n" + "]\n"
			+ "# 表名: 包邮范围, 包含字段:\n" + "[\n" + "  (快递公司id),\n" + "  (发货区域),\n" + "  (包邮区域)\n" + "]\n" + "\n" + "【多轮输入】\n"
			+ ">用户：网店信息？\n" + ">助理：请问您是想列举出所有站点的信息吗？\n" + "<最新>用户：对\n" + "\n" + "【输出】\n" + "需求类型：《数据分析》\n"
			+ "语种类型：《中文》\n" + "需求内容：列举出所有快递站点的信息\n" + "\n" + "=======\n" + "\n" + "【数据库】\n" + "库名: 医院信息, 包含以下表:\n"
			+ "# 表名: 医院, 医院基础信息, 包含字段:\n" + "[\n" + "  (医院id),\n"
			+ "  (医院名, 示例值:['中国人民解放军海军军医大学', '哈尔滨医科大学第三临床医学院', '北京人民医院', '吉林大学白求恩第一医院', '天津市肿瘤医院']),\n"
			+ "  (所属城市id),\n" + "  (医院等级),\n" + "  (employees: number, 职工数量),\n" + "  (院士数量),\n" + "  (科室数量),\n"
			+ "  (重点专科数量)\n" + "]\n" + "# 表名: 医院排名, 包含字段:\n" + "[\n" + "  (年份),\n" + "  (医院id),\n" + "  (排名),\n"
			+ "  (接待病患数量),\n" + "  (手术数量)\n" + "]\n" + "# 表名: 城市, 包含字段:\n" + "[\n" + "  (城市id),\n"
			+ "  (名称, 示例值:['哈尔滨', '桂林', '铁岭', '赤峰', '洛阳']),\n" + "  (所属省份, 示例值:['黑龙江', '辽宁', '河南', '广西', '内蒙古']),\n"
			+ "  (人口数量),\n" + "  (Elderly population ratio, 老年人占比)\n" + "]\n" + "# 表名: 特色科室, 包含字段:\n" + "[\n"
			+ "  (科室),\n" + "  (医院id),\n" + "  (是否重点, 示例值:['是', '否']),\n" + "  (是否研究中心)\n" + "]\n" + "\n" + "【多轮输入】\n"
			+ ">用户：杭州人民医院在去年的排名是多少？\n" + "<最新>用户：这个医院今年的排名呢\n" + "\n" + "【输出】\n" + "需求类型：《数据分析》\n" + "语种类型：《中文》\n"
			+ "需求内容：杭州人民医院今年的排名\n" + "\n" + "=======\n" + "\n" + "现在请回答下面的：\n" + "\n" + "【数据库】\n" + "{db_content}\n"
			+ "\n" + "【多轮输入】\n" + "{multi_turn}\n" + "\n" + "【参考信息】\n" + "{evidence}\n" + "【输出】\n" + "需求类型：\n"
			+ "语种类型：\n" + "需求内容：";

	public static final PromptTemplate INIT_REWRITE_WITH_CLARIFY_AND_EVIDENCE_PROMPT_TEMPLATE = new PromptTemplate(
			INIT_REWRITE_WITH_AND_EVIDENCE_CLARIFY_PROMPT_STRING);

	public static final String QUESTION_TO_KEYWORDS_PROMPT_STRING = "将下述问题的关键语料抽取出来，直接以list形式输出，不要分析。\n" + "示例如下：\n"
			+ "【问题】\n" + "查询2024年8月在北京，一级标签为“未成单”的人数。\n" + "【关键语料】\n"
			+ "[\"2024年8月\", \"北京\", \"一级标签\", \"未成单\", \"人数\"]\n" + "\n" + "【问题】\n"
			+ "Name movie titles released in year 1945. Sort the listing by the descending order of movie popularity. released in the year 1945 refers to movie_release_year = 1945;\n"
			+ "【关键语料】\n"
			+ "[\"movie titles\", \"released in year 1945\", \"movie popularity\", \"movie_release_year = 1945\"]\n"
			+ "\n" + "【问题】\n"
			+ "List all product name from Australia Bike Retailer order by product ID. Australia Bike Retailer is name of vendor\n"
			+ "【关键语料】\n" + "[\"product name\", \"Australia Bike Retailer\", \"product ID\", \"name of vendor\"]\n"
			+ "\n" + "【问题】\n" + "山东省济南市各车型（牵引车、载货车、自卸车、搅拌车）销量占比的月趋势\n" + "【关键语料】\n"
			+ "[\"山东省\", \"济南市\", \"各车型\", \"牵引车\", \"载货车\", \"自卸车\", \"搅拌车\", \"销量占比\", \"月趋势\"]\n" + "\n" + "【问题】\n"
			+ "{question}\n" + "【关键语料】";

	public static final PromptTemplate QUESTION_TO_KEYWORDS_PROMPT_TEMPLATE = new PromptTemplate(
			QUESTION_TO_KEYWORDS_PROMPT_STRING);

	public static final String MIX_SELECTOR_PROMPT_STRING = "你现在是一位数据分析师，你的任务是分析用户的问题和数据库schema，数据库schema包括表名、表描述、表之间的外键依赖，每张表中包含多个列的列名、列描述和主键信息，现在你需要根据提供的数据库信息和用户的问题，分析与用户问题相关的table，给出相关table的名称。\n"
			+ "[Instruction]:\n" + "1. 排除与用户问题完全不相关的table\n" + "2. 保留可能对回答用户问题有帮助的表\n"
			+ "3. 结果以json形式输出，用```json和```包围起来\n" + "4. 直接输出结果，不要做多余的分析\n" + "\n" + "以下样例供你参考：\n" + "\n"
			+ "【DB_ID】 station_weather\n" + "# Table: train\n" + "[\n" + "(id:TEXT, 火车编号, Primary Key.),\n"
			+ "(train_number:TEXT, 火车车次, Examples: [56701]),\n" + "(name:TEXT, 火车的名称),\n"
			+ "(origin:TEXT, 出发站, Examples: [Kanniyakumari, Chennai, Trivandrum]),\n"
			+ "(destination:TEXT, 到达站, Examples: [Kanniyakumari, Chennai, Trivandrum]),\n"
			+ "(time:TEXT, 发车时间, Examples: [4:49, 22:10, 21:49]),\n" + "(interval:TEXT, 火车的运行频率, Examples: [Daily])\n"
			+ "]\n" + "# Table: station\n" + "[\n" + "(id:TEXT, 车站编号, Primary Key),\n"
			+ "(network_name:TEXT, 车站所属网络的名称, Examples: [Croxley, Chorleywood, Cheshunt]),\n"
			+ "(services:TEXT, 提供的服务),\n"
			+ "(local_authority:TEXT, 负责该车站区域的地方当局, Examples: [Three Rivers, Chiltern, Broxbourne])\n" + "]\n"
			+ "# Table: route\n" + "[\n" + "(train_id:TEXT, 火车编号),\n" + "(station_id:TEXT, 车站编号)\n" + "]\n"
			+ "# Table: weekly_weather\n" + "[\n" + "(station_id::TEXT, 车站编号),\n"
			+ "(day_of_week:TEXT, 星期, Examples: [Tuesday, Monday, Wednesday]),\n"
			+ "(high_temperature:INT, 最高气温, Examples: [59, 55, 58]),\n"
			+ "(low_temperature:INT, 最低气温, Examples: [54, 52, 55]),\n"
			+ "(precipitation:DOUBLE, 降水量, Examples: [50.0, 90.0, 70.0]),\n"
			+ "(wind_speed_mph:INT, 风速, Examples: [22, 14, 13])\n" + "]\n" + "【Foreign keys】\n"
			+ "route.station_id=station.id\n" + "route.train_id=train.id\n" + "weekly_weather.station_id=station.id\n"
			+ "\n" + "【问题】\n" + "How many different services are provided by all stations?\n" + "【Answer】\n"
			+ "```json\n" + "[\"station\"]\n" + "```\n" + "\n" + "=============\n" + "【DB_ID】 hr_1\n"
			+ "# Table: regions\n" + "[\n" + "(REGION_ID:TEXT, Primary Key),\n" + "(REGION_NAME:TEXT)\n" + "]\n"
			+ "# Table: countries\n" + "[\n" + "(COUNTRY_ID:TEXT, Primary Key),\n" + "(COUNTRY_NAME:TEXT),\n"
			+ "(REGION_ID:TEXT)\n" + "]\n" + "# Table: departments\n" + "[\n" + "(DEPARTMENT_ID:TEXT, Primary Key),\n"
			+ "(DEPARTMENT_NAME:TEXT, department name. Examples: [Treasury, Shipping, Shareholder Services]),\n"
			+ "(MANAGER_ID:TEXT),\n" + "(LOCATION_ID:TEXT)\n" + "]\n" + "# Table: jobs\n" + "[\n"
			+ "(JOB_ID:TEXT, Primary Key),\n" + "(JOB_TITLE:TEXT),\n"
			+ "(MIN_SALARY:INT, min salary. Examples: [4000, 8200, 4200]),\n"
			+ "(MAX_SALARY:INT, max salary. Examples: [9000, 16000, 15000])\n" + "]\n" + "# Table: employees\n" + "[\n"
			+ "(EMPLOYEE_ID:TEXT),\n" + "(FIRST_NAME:TEXT, Examples: [Peter, John, David]),\n"
			+ "(LAST_NAME:TEXT, Examples: [Taylor, Smith, King]),\n" + "(EMAIL:TEXT),\n"
			+ "(PHONE_NUMBER:TEXT, Examples: [650.509.4876]),\n" + "(HIRE_DATE:DATE, Examples: [1987-10-01]),\n"
			+ "(JOB_ID:TEXT),\n" + "(SALARY:TEXT, Examples: [2500, 10000, 9000]),\n"
			+ "(COMMISSION_PCT:DOUBLE, Examples: [0, 0.3, 0.25]),\n" + "(MANAGER_ID:TEXT),\n" + "(DEPARTMENT_ID:TEXT)\n"
			+ "]\n" + "# Table: job_history\n" + "[\n" + "(EMPLOYEE_ID:TEXT),\n"
			+ "(START_DATE:DATE, Examples: [1999-01-01]),\n" + "(END_DATE:DATE, Examples: [1999-12-31]),\n"
			+ "(JOB_ID:TEXT),\n" + "(DEPARTMENT_ID:TEXT)\n" + "]\n" + "# Table: locations\n" + "[\n"
			+ "(LOCATION_ID:TEXT),\n" + "(STREET_ADDRESS:TEXT),\n"
			+ "(POSTAL_CODE:TEXT, Examples: [YSW 9T2, M5V 2L7, 99236]),\n"
			+ "(CITY:TEXT, Examples: [Whitehorse, Venice, Utrecht]),\n"
			+ "(STATE_PROVINCE:TEXT, Examples: [Yukon, Washington, Utrecht]),\n" + "(COUNTRY_ID:TEXT)\n" + "]\n"
			+ "【Foreign keys】\n" + "countries.REGION_ID=regions.REGION_ID\n" + "employees.JOB_ID=jobs.JOB_ID\n"
			+ "employees.DEPARTMENT_ID=departments.DEPARTMENT_ID\n" + "job_history.JOB_ID=jobs.JOB_ID\n"
			+ "job_history.DEPARTMENT_ID=departments.DEPARTMENT_ID\n"
			+ "job_history.EMPLOYEE_ID=employees.EMPLOYEE_ID\n" + "locations.COUNTRY_ID=countries.COUNTRY_ID\n" + "\n"
			+ "【问题】\n"
			+ "display the full name (first and last name ) of employee with ID and name of the country presently where (s)he is working.\n"
			+ "【Answer】\n" + "```json\n" + "[\"employees\", \"departments\", \"countries\", \"locations\"]\n" + "```\n"
			+ "=============\n" + "{schema_info}\n" + "\n" + "【问题】\n" + "{question}\n" + "【参考信息】\n" + "{evidence}\n"
			+ "【Answer】";

	public static final PromptTemplate MIX_SELECTOR_PROMPT_TEMPLATE = new PromptTemplate(MIX_SELECTOR_PROMPT_STRING);

	public static final String MIX_SQL_GENERATOR_SYSTEM_PROMPT_STRING = "现在你是一个{dialect}生成师，需要阅读一个客户的问题，参考的数据库schema，根据参考信息的提示，生成一句可执行的SQL。\n"
			+ "注意：\n" + "1、不要select多余的列。\n" + "2、生成的SQL用```sql 和```包围起来。\n" + "3、不要在SQL语句中加入注释！！！\n" + "\n"
			+ "【数据库schema】\n" + "{schema_info}\n" + "\n" + "【参考信息】\n" + "{evidence}";

	public static final PromptTemplate MIX_SQL_GENERATOR_SYSTEM_PROMPT_TEMPLATE = new PromptTemplate(
			MIX_SQL_GENERATOR_SYSTEM_PROMPT_STRING);

	public static final String MIX_SQL_GENERATOR_PROMPT_STRING = "【问题】\n" + "{question}\n" + "\n" + "【SQL】";

	public static final PromptTemplate MIX_SQL_GENERATOR_PROMPT_TEMPLATE = new PromptTemplate(
			MIX_SQL_GENERATOR_PROMPT_STRING);

	public static final String EXTRACT_DATETIME_PROMPT_STRING = "给你一个用户的问题，请你提取出该用户所提问的时间，时间维度包括：年、月、日，结果以list格式输出。形如：[\"今年\", \"本月\", \"今日\"] \n，如果用户的问题不涉及时间，直接返回给我[],不需要多余的内容！不要回答多余的内容！"
			+ "用户问题：{question}\n回答：";

	public static final PromptTemplate EXTRACT_DATETIME_PROMPT_TEMPLATE = new PromptTemplate(
			EXTRACT_DATETIME_PROMPT_STRING);

}
