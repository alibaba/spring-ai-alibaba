/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.prompt;

import org.springframework.ai.chat.prompt.PromptTemplate;

public class PromptConstant {

	public static final String INIT_REWRITE_PROMPT_STRING = """
			你是一名数据分析专家，需要从用户和助理的对话中确定用户最新需求的需求类型和需求内容，你需要遵循：
			1. 判断用户的需求类型请从【需求类型】中进行选择。
			2. 需求内容的生成，请严格按照语种类型生成，如果语种类型为《中文》，则以中文生成需求内容，如果语种类型为《英文》，则以英文生成需求内容。
			3. 需求内容的生成，需要进行必要的继承上文内容替换改写，特别是对<最新>中出现的指代（如这、这个、那等）进行名词替换；注意不要遗漏相关信息，但不要改变用户问题的原意，不要过度的解释说明，不需要再重复输出用户的历史需求。
			4. 当用户<最新>输入是回答助理的问题时，如果基于用户的回答已经能够明确意图，可以直接生成需求内容。当需要澄清时，需求内容为需要向用户反问的问题.

			【需求类型】
			《数据分析》：用户需求与【数据库】中的内容较为相关，能够通过查询数据库或对查询结果进行数据分析来满足用户需求
			《需要澄清》: 用户需求与【数据库】中的内容较为相关，但是所提供的信息不足以完成查询任务，需要用户提供更多信息
			《自由闲聊》：开放域的自由聊天和不涉及【数据库】中相关内容的数据查询分析

			【语种类型】
			《中文》
			《英文》

			下面是一些参考样例：
			=======
			【数据库】
			库名: pets_1, 包含以下表:
			# 表名: 学生, 包含字段:
			[
			  (学生ID),
			  (姓氏),
			  (名字),
			  (年龄),
			  (性别),
			  (专业),
			  (导师),
			  (城市代码)
			]
			# 表名: 养宠物, 包含字段:
			[
			  (学生ID),
			  (宠物ID)
			]
			# 表名: 宠物, 包含字段:
			[
			  (宠物ID),
			  (宠物类型),
			  (宠物年龄),
			  (体重)
			]

			【多轮输入】
			>用户：小李的小鸡有多重啊
			>用户：这个宠物多大了？
			<最新>用户：I like this one so much, haha.

			【输出】
			需求类型：《自由闲聊》
			语种类型：《英文》
			需求内容：I like this little chicken pet.

			=======
			【数据库】
			库名: 广州博奥有限公司工资, 主要为公司工资相关的信息库, 包含以下表:
			# 表名: 事业群, 包含字段:
			[
			  (事业群编号),
			  (事业群名, 示例值:['基础平台', '游戏', '商业联盟', '文娱', '智能产品']),
			  (负责人, 示例值:['张晓阳', '曾南', '李一乐', '张艺', '马可']),
			  (员工数量),
			  (目标营收),
			  (成本支出)
			]
			# 表名: 部门人员, 包含字段:
			[
			  (部门编号),
			  (年份),
			  (员工数量),
			  (离职人数),
			  (招聘人数)
			]
			# 表名: 部门, 包含字段:
			[
			  (部门编号),
			  (部门名称, 示例值:['搜索策略部门', '点击预估部门', '用户产品调研部', '基础架构部', '广告品牌部']),
			  (职责, 示例值:['机器管理及部署', '负责广告分发及维护', '广告排序', '用户目标群体调研及产品设计', '搜索基础策略研发及优化']),
			  (所属群编号)
			]
			# 表名: 员工, 包含字段:
			[
			  (员工编号),
			  (姓名, 示例值:['陈晓程', '江夏', '张天天', '田小雨', '李乐乐']),
			  (职称, 示例值:['高级工程师', '架构工程师', '资深工程师', '工程师']),
			  (薪资),
			  (年龄),
			  (工龄),
			  (所属部门id)
			]

			【多轮输入】
			>用户：按工资降序对员工进行排列。
			>用户：哪个工资最高？
			>用户：最低的呢？
			>用户：他们分别来自哪个部门？
			<最新>用户：这些部门的职责

			【输出】
			需求类型：《数据分析》
			语种类型：《中文》
			需求内容：工资最高的和工资最低的员工的所属部门的职责分别是什么？

			=======
			【数据库】
			库名: world_1, World Information, 包含以下表:
			# Table: city, 包含字段:
			[
			  (city id),
			  (name, Value examples: ['New York', 'paris', 'Beijing']),
			  (country code),
			  (district),
			  (population)
			]
			# Table: sequence, 包含字段:
			[
			  (名称),
			  (序列)
			]
			# Table: country, 包含字段:
			[
			  (Code),
			  (Name),
			  (Continent),
			  (Region),
			  (Area),
			  (Independence Year),
			  (Population),
			  (Life Expectancy),
			  (GDP, 国民生产总值.),
			  (Old GDP),
			  (Local Name),
			  (Government Form),
			  (Leader),
			  (Capital),
			  (Code2)
			]
			# Table: country_language, 包含字段:
			[
			  (country_code),
			  (语言),
			  (is_official),
			  (percentage),
			  (is_MM)
			]

			【多轮输入】
			>用户：List all MM languages.
			<最新>用户：最古老的是哪个？

			【输出】
			需求类型：《自由闲聊》
			语种类型：《中文》
			需求内容：最古老的MM语言是哪个？

			=======
			【数据库】
			库名: 网易云阅读, 包含以下表:
			# 表名: 作者, 包含字段:
			[
			  (作者id),
			  (姓名, 示例值:['梁夜白', '唐七公子', '吱吱', '匪我思存', '林绾绾']),
			  (年龄)
			]
			# 表名: 出版图书, 包含字段:
			[
			  (出版图书id),
			  (书名, 示例值:['激荡三十年', '贫穷的本质', '人类简史', '三体', '熊镇']),
			  (作者id),
			  (评分),
			  (评价人数),
			  (字数),
			  (点击数),
			  (类型, 示例值:['人文社科', '经济管理', '小说', '科技书籍'])
			]
			# 表名: 网络小说, 包含字段:
			[
			  (网络图书id),
			  (书名, 示例值:['三生三世十里桃花', '爱情的开关', '重生之锦绣皇后', '医妃权倾天下', '蓝桥几顾']),
			  (作者id),
			  (评分),
			  (类型, 示例值:['穿越', '种田', '同人', '古言', '现言']),
			  (状态, 示例值:['完结', '更新中']),
			  (价格)
			]
			# 表名: 畅销榜, 包含字段:
			[
			  (网络小说id),
			  (周排名),
			  (月排名),
			  (总排名),
			  (省份)
			]
			# 表名: 收藏榜, 包含字段:
			[
			  (网络小说id),
			  (周排名),
			  (月排名),
			  (总排名)
			]

			【多轮输入】
			>用户：列出已经更新完毕的网络小说有哪些？
			>用户：列出销量最好的省份
			<最新>用户：这个省哪个书今年三月份排名最高

			【输出】
			需求类型：《数据分析》
			语种类型：《中文》
			需求内容：销量最好的省份哪个书今年三月份排名最高

			=======
			【数据库】
			库名: 快递公司, 包含以下表:
			# 表名: 快递公司, 包含字段:
			[
			  (公司id),
			  (公司名, Value examples: ['顺丰', '韵达', '圆通', '中通'].),
			  (总部地点, Value examples: ['深圳', '杭州'].),
			  (成立时间, Value examples: ['2010年', '1999年'].),
			  (员工数量),
			  (运输车辆数),
			  (覆盖城市数量),
			  (网点数量)
			]
			# 表名: 省份, 包含字段:
			[
			  (省id),
			  (省名, Value examples: ['浙江', '江西', '上海', '北京'].),
			  (所属区域, Value examples: ['长三角', '东北', '西北'].)
			]
			# 表名: 快递费, 包含字段:
			[
			  (快递公司id),
			  (区域),
			  (起步价格),
			  (起步公斤数),
			  (每公斤价格)
			]
			# 表名: 站点表, 包含字段:
			[
			  (驿站id),
			  (收件总数),
			  (驿站状态。0：正常，1：关闭),
			  (驿站名字),
			  (驿站类型。0 自营。1 加盟。3 注资),
			  (所属公司，Value examples: ['顺丰', '申通', '韵达'])
			]
			# 表名: 包邮范围, 包含字段:
			[
			  (快递公司id),
			  (发货区域),
			  (包邮区域)
			]

			【多轮输入】
			>用户：网店信息？
			>助理：请问您是想列举出所有站点的信息吗？
			<最新>用户：对

			【输出】
			需求类型：《数据分析》
			语种类型：《中文》
			需求内容：列举出所有快递站点的信息

			=======
			【数据库】
			库名: 医院信息, 包含以下表:
			# 表名: 医院, 医院基础信息, 包含字段:
			[
			  (医院id),
			  (医院名, 示例值:['中国人民解放军海军军医大学', '哈尔滨医科大学第三临床医学院', '北京人民医院', '吉林大学白求恩第一医院', '天津市肿瘤医院']),
			  (所属城市id),
			  (医院等级),
			  (employees: number, 职工数量),
			  (院士数量),
			  (科室数量),
			  (重点专科数量)
			]
			# 表名: 医院排名, 包含字段:
			[
			  (年份),
			  (医院id),
			  (排名),
			  (接待病患数量),
			  (手术数量)
			]
			# 表名: 城市, 包含字段:
			[
			  (城市id),
			  (名称, 示例值:['哈尔滨', '桂林', '铁岭', '赤峰', '洛阳']),
			  (所属省份, 示例值:['黑龙江', '辽宁', '河南', '广西', '内蒙古']),
			  (人口数量),
			  (Elderly population ratio, 老年人占比)
			]
			# 表名: 特色科室, 包含字段:
			[
			  (科室),
			  (医院id),
			  (是否重点, 示例值:['是', '否']),
			  (是否研究中心)
			]

			【多轮输入】
			>用户：杭州人民医院在去年的排名是多少？
			<最新>用户：这个医院今年的排名呢

			【输出】
			需求类型：《数据分析》
			语种类型：《中文》
			需求内容：杭州人民医院今年的排名

			=======
			现在请回答下面的：

			【数据库】
			%s

			【多轮输入】
			%s

			【参考信息】
			%s

			【输出】
			需求类型：
			语种类型：
			需求内容：
			"""
		.formatted("{db_content}", "{multi_turn}", "{evidence}");

	public static final PromptTemplate INIT_REWRITE_PROMPT_TEMPLATE = new PromptTemplate(INIT_REWRITE_PROMPT_STRING);

	public static final String QUESTION_TO_KEYWORDS_PROMPT_STRING = """
			将下述问题的关键语料抽取出来，直接以list形式输出，不要分析。
			示例如下：
			【问题】
			查询2024年8月在北京，一级标签为“未成单”的人数。
			【关键语料】
			["2024年8月", "北京", "一级标签", "未成单", "人数"]

			【问题】
			Name movie titles released in year 1945. Sort the listing by the descending order of movie popularity. released in the year 1945 refers to movie_release_year = 1945;
			【关键语料】
			["movie titles", "released in year 1945", "movie popularity", "movie_release_year = 1945"]

			【问题】
			List all product name from Australia Bike Retailer order by product ID. Australia Bike Retailer is name of vendor
			【关键语料】
			["product name", "Australia Bike Retailer", "product ID", "name of vendor"]

			【问题】
			山东省济南市各车型（牵引车、载货车、自卸车、搅拌车）销量占比的月趋势
			【关键语料】
			["山东省", "济南市", "各车型", "牵引车", "载货车", "自卸车", "搅拌车", "销量占比", "月趋势"]

			【问题】
			%s
			【关键语料】
			"""
		.formatted("{question}");

	public static final PromptTemplate QUESTION_TO_KEYWORDS_PROMPT_TEMPLATE = new PromptTemplate(
			QUESTION_TO_KEYWORDS_PROMPT_STRING);

	public static final String MIX_SELECTOR_PROMPT_STRING = """
			你现在是一位数据分析师，你的任务是分析用户的问题和数据库schema，数据库schema包括表名、表描述、表之间的外键依赖，每张表中包含多个列的列名、列描述和主键信息，现在你需要根据提供的数据库信息和用户的问题，分析与用户问题相关的table，给出相关table的名称。
			[Instruction]:
			1. 排除与用户问题完全不相关的table
			2. 保留可能对回答用户问题有帮助的表
			3. 结果以json形式输出，用```json和```包围起来
			4. 直接输出结果，不要做多余的分析

			以下样例供你参考：

			【DB_ID】 station_weather
			# Table: train
			[
			(id:TEXT, 火车编号, Primary Key.),
			(train_number:TEXT, 火车车次, Examples: [56701]),
			(name:TEXT, 火车的名称),
			(origin:TEXT, 出发站, Examples: [Kanniyakumari, Chennai, Trivandrum]),
			(destination:TEXT, 到达站, Examples: [Kanniyakumari, Chennai, Trivandrum]),
			(time:TEXT, 发车时间, Examples: [4:49, 22:10, 21:49]),
			(interval:TEXT, 火车的运行频率, Examples: [Daily])
			]
			# Table: station
			[
			(id:TEXT, 车站编号, Primary Key),
			(network_name:TEXT, 车站所属网络的名称, Examples: [Croxley, Chorleywood, Cheshunt]),
			(services:TEXT, 提供的服务),
			(local_authority:TEXT, 负责该车站区域的地方当局, Examples: [Three Rivers, Chiltern, Broxbourne])
			]
			# Table: route
			[
			(train_id:TEXT, 火车编号),
			(station_id:TEXT, 车站编号)
			]
			# Table: weekly_weather
			[
			(station_id::TEXT, 车站编号),
			(day_of_week:TEXT, 星期, Examples: [Tuesday, Monday, Wednesday]),
			(high_temperature:INT, 最高气温, Examples: [59, 55, 58]),
			(low_temperature:INT, 最低气温, Examples: [54, 52, 55]),
			(precipitation:DOUBLE, 降水量, Examples: [50.0, 90.0, 70.0]),
			(wind_speed_mph:INT, 风速, Examples: [22, 14, 13])
			]
			【Foreign keys】
			route.station_id=station.id
			route.train_id=train.id
			weekly_weather.station_id=station.id

			【问题】
			How many different services are provided by all stations?
			【Answer】
			```json
			["station"]
			```

			===============
			【DB_ID】 hr_1
			# Table: regions
			[
			(REGION_ID:TEXT, Primary Key),
			(REGION_NAME:TEXT)
			]
			# Table: countries
			[
			(COUNTRY_ID:TEXT, Primary Key),
			(COUNTRY_NAME:TEXT),
			(REGION_ID:TEXT)
			]
			# Table: departments
			[
			(DEPARTMENT_ID:TEXT, Primary Key),
			(DEPARTMENT_NAME:TEXT, department name. Examples: [Treasury, Shipping, Shareholder Services]),
			(MANAGER_ID:TEXT),
			(LOCATION_ID:TEXT)
			]
			# Table: jobs
			[
			(JOB_ID:TEXT, Primary Key),
			(JOB_TITLE:TEXT),
			(MIN_SALARY:INT, min salary. Examples: [4000, 8200, 4200]),
			(MAX_SALARY:INT, max salary. Examples: [9000, 16000, 15000])
			]
			# Table: employees
			[
			(EMPLOYEE_ID:TEXT),
			(FIRST_NAME:TEXT, Examples: [Peter, John, David]),
			(LAST_NAME:TEXT, Examples: [Taylor, Smith, King]),
			(EMAIL:TEXT),
			(PHONE_NUMBER:TEXT, Examples: [650.509.4876]),
			(HIRE_DATE:DATE, Examples: [1987-10-01]),
			(JOB_ID:TEXT),
			(SALARY:TEXT, Examples: [2500, 10000, 9000]),
			(COMMISSION_PCT:DOUBLE, Examples: [0, 0.3, 0.25]),
			(MANAGER_ID:TEXT),
			(DEPARTMENT_ID:TEXT)
			]
			# Table: job_history
			[
			(EMPLOYEE_ID:TEXT),
			(START_DATE:DATE, Examples: [1999-01-01]),
			(END_DATE:DATE, Examples: [1999-12-31]),
			(JOB_ID:TEXT),
			(DEPARTMENT_ID:TEXT)
			]
			# Table: locations
			[
			(LOCATION_ID:TEXT),
			(STREET_ADDRESS:TEXT),
			(POSTAL_CODE:TEXT, Examples: [YSW 9T2, M5V 2L7, 99236]),
			(CITY:TEXT, Examples: [Whitehorse, Venice, Utrecht]),
			(STATE_PROVINCE:TEXT, Examples: [Yukon, Washington, Utrecht]),
			(COUNTRY_ID:TEXT)
			]
			【Foreign keys】
			countries.REGION_ID=regions.REGION_ID
			employees.JOB_ID=jobs.JOB_ID
			employees.DEPARTMENT_ID=departments.DEPARTMENT_ID
			job_history.JOB_ID=jobs.JOB_ID
			job_history.DEPARTMENT_ID=departments.DEPARTMENT_ID
			job_history.EMPLOYEE_ID=employees.EMPLOYEE_ID
			locations.COUNTRY_ID=countries.COUNTRY_ID

			【问题】
			display the full name (first and last name ) of employee with ID and name of the country presently where (s)he is working.
			【Answer】
			```json
			["employees", "departments", "countries", "locations"]
			```

			===============
			%s

			【问题】
			%s

			【参考信息】
			%s

			【Answer】
			"""
		.formatted("{schema_info}", "{question}", "{evidence}");

	public static final PromptTemplate MIX_SELECTOR_PROMPT_TEMPLATE = new PromptTemplate(MIX_SELECTOR_PROMPT_STRING);

	public static final String MIX_SQL_GENERATOR_SYSTEM_PROMPT_STRING = """
			现在你是一个%s生成师，需要阅读一个客户的问题，参考的数据库schema，根据参考信息的提示，生成一句可执行的SQL。
			注意：
			1、不要select多余的列。
			2、生成的SQL用```sql 和```包围起来。
			3、不要在SQL语句中加入注释！！！

			【数据库schema】
			%s

			【参考信息】
			%s
			""".formatted("{dialect}", "{schema_info}", "{evidence}");

	public static final PromptTemplate MIX_SQL_GENERATOR_SYSTEM_PROMPT_TEMPLATE = new PromptTemplate(
			MIX_SQL_GENERATOR_SYSTEM_PROMPT_STRING);

	public static final String MIX_SQL_GENERATOR_PROMPT_STRING = """
			【问题】
			%s

			【SQL】
			""".formatted("{question}");

	public static final PromptTemplate MIX_SQL_GENERATOR_PROMPT_TEMPLATE = new PromptTemplate(
			MIX_SQL_GENERATOR_PROMPT_STRING);

	public static final String EXTRACT_DATETIME_PROMPT_STRING = """
			给你一个用户的问题，请你提取出该用户所提问的时间，时间维度包括：年、月、日，结果以list格式输出。形如：["今年", "本月", "今日"]
			，如果用户的问题不涉及时间，直接返回给我[]，不需要多余的内容！不要回答多余的内容！
			用户问题：%s
			回答：
			""".formatted("{question}");

	public static final PromptTemplate EXTRACT_DATETIME_PROMPT_TEMPLATE = new PromptTemplate(
			EXTRACT_DATETIME_PROMPT_STRING);

	public static final String SEMANTIC_CONSISTENC_PROMPT_STRING = """
			# 角色
			您是一位注重实效的SQL审计助手，核心目标是判断SQL是否满足业务需求主干。在保证数据准确性的前提下，允许存在可通过简单修改调整的非核心问题。

			# 业务背景
			## 用户需求：
			%s
			## 待验证SQL：
			%s

			# 审计原则
			1. **核心需求优先**：仅关注影响结果准确性的关键要素
			2. **允许合理偏差**：接受不影响业务决策的细微差异
			3. **优化建议分离**：将改进建议与通过判定分离

			# 审计维度
			## 1. 核心逻辑验证
			- ✅ **关键过滤条件**：时间范围、状态值等影响结果主干的条件
			- ✅ **核心计算逻辑**：SUM/COUNT等聚合函数是否本质正确
			- ✅ **主字段覆盖**：是否包含业务决策必需字段

			## 2. 弹性接受项
			- ➡️ 非关键字段缺失/多余（不影响业务解读）
			- ➡️ 排序规则偏差（非核心排序需求）
			- ➡️ 语法优化项（不影响结果正确性）

			## 3. 问题分级
			-  **致命问题**：结果错误、核心逻辑缺失
			-  **可修复问题**：需简单调整的非核心问题
			-  **优化建议**：代码规范等非功能性改进

			# 不通过判定标准
			仅当存在以下情况时判定不通过：
			1. 核心业务逻辑错误（如错误聚合计算）
			2. 关键过滤条件缺失导致结果失真
			3. 结构缺陷导致无法通过简单修改修复

			# 输出格式
			请严格只返回“通过”或“不通过，并附具体原因”。


			""".formatted("{nl_req}", "{sql}");

	public static final PromptTemplate SEMANTIC_CONSISTENC_PROMPT_TEMPLATE = new PromptTemplate(
			SEMANTIC_CONSISTENC_PROMPT_STRING);

	public static final String MIX_SQL_GENERATOR_SYSTEM_PROMPT_CHECK_STRING = """
			现在你是一个%s生成师，请评估以下内容能否生成可执行SQL：

			### 核心审查原则（优先级从高到低）：
			1. **表必须存在** \s
			   - 问题直接提及的表必须在schema中定义
			   - 衍生表（如子查询结果）无需预先存在

			2. **基础字段验证** \s
			   - 计算所需的原始字段必须存在（如计算总价需要单价和数量）
			   - 业务术语允许映射（如状态值可通过参考信息解释）

			3. **智能连接推导** \s
			   - 通过业务语义自动关联实体（如涉及多实体时推导主外键关系）
			   - 参考信息中声明的连接关系直接采纳
			   - 仅当多表查询且完全无法推导连接逻辑时才判否

			4. **操作可行性检查** \s
			   - 聚合操作只需基础字段存在且类型合理
			   - 状态过滤支持业务术语到字段值的映射

			### 硬性缺失判定标准（任一不满足即返回"否"）：
			❌ 问题直接提及的表缺失 \s
			❌ 计算所需的基础字段完全不存在 \s
			❌ 多表查询且连接逻辑完全无法推导 \s
			❌ 关键过滤条件字段缺失且无法映射 \s

			  ### 返回格式（严格只允许如下两种）：
			  - **全部满足上述条件** → 返回：“是”
			  - **任一条件不满足** → 返回：“否”，并附具体原因（不超过1句话）

			  ---
			  【数据库schema】
			  %s

			  【参考信息】
			  %s

			  【客户问题】
			  %s

			  ---
			  【审查结果】
			   请严格只返回“是”或“否，并附具体原因（不超过1句话）”。
			""".formatted("{dialect}", "{schema_info}", "{evidence}", "{question}");

	public static final PromptTemplate MIX_SQL_GENERATOR_SYSTEM_PROMPT_CHECK_TEMPLATE = new PromptTemplate(
			MIX_SQL_GENERATOR_SYSTEM_PROMPT_CHECK_STRING);

}
