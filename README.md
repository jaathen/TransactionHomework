# 交易系统
## 项目概述
一个基于内存的交易管理系统，支持增、删、改和分页查询
## 技术栈
* SpringBoot 3.4.2
* jdk 21
## 快速开始
* 使用docker方式部署
```
./deploy/build.sh
./deploy/run.sh
```
## 压测相关
### 压测环境及参数介绍
基于本地笔记本虚拟化后进行压测，配置4C8G
压测工具 jmeter
参数
500线程并发、每个线程执行1000个请求，逐个接口进行压测


| 接口   | 平均响应时间 | 最大响应时间 |错误率| QPS   | CPU使用率 |备注|
|------|-----|--------|----|-------|--------|----|
| 创建交易 | 10ms | 250ms  |0.02%| 36032 | 298%   |
| 更新交易 | 14ms| 266ms  |0.00%| 32814 | 308%   |
| 删除交易 | 12ms| 147ms  |0.00%| 40025 | 275%   |
| 查询   | 12ms| 147ms  |0.00%| 40025 | 275%   |

