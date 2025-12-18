# 一个史诗级巨作



需要先在docker中安装两个MySQL节点,一定要用5.4的，我已经给出了镜像文件，直接运行即可，kafka，zookeeper，kafka-ui这些镜像  



***java的版本一定是jdk8***



然后在本地运行mycat（由于mycat的docker镜像不是很稳定所以只好在本地运行了）



接着在前端部分启动项目 npm run dev（在命令行中启动，如cmd）



在后端部分使用Intellij IDEA一键启动（或者打包成jar包之后启动，由于暂时不需要部署，所以暂不打包）



在数据生成部分，使用pycharm，运行use-this.py代码，



docker中确保容器全部启动。创建容器的配置文件也增加了进来



后端代码如果有更改需求千万别乱改，一定让我本人亲自来改





---

## 详细版教程

如何运行该项目，首先在前端所在的文件夹打开cmd命令框，输入npm run dev

在docker中将需要的容器全部启动，同时启动mycat服务，mycat的启动方法同样是通过命令行，在mycat的bin目录下打开cmd输入mycat console即可启动。（记得一定要先把conf目录下的三个配置文件进行替换）

使用Intellij IDEA打开backend文件夹，之后打开mycat，mysql的两个节点，确保建好了库和表

至于如何建表，可以使用Intellij IDEA自带的数据库连接功能连接到mycat，两个mysql，由于Intellij IDEA内置的datagrip对mycat识别不佳，所以可以直接在两个mysql中创建库和表

具体如下

![bd4dde6a-3931-4043-a221-ee5923b8e89e](file:///C:/Users/26515/Pictures/Typedown/bd4dde6a-3931-4043-a221-ee5923b8e89e.png)

mysql节点1创建数据库db1，节点2建里db2，然后相应的建表语句如下

```sql
create table etc_data
(
    id             bigint         not null comment '主键ID'
        primary key,
    plate_number   varchar(20)    null comment '车牌号码',
    plate_type     varchar(20)    null comment '号牌种类',
    pass_time      datetime       null comment '过车时间',
    bayonet_name   varchar(100)   null comment '卡口名称',
    district_name  varchar(64)    null comment '行政区划',
    direction_type varchar(20)    null comment '方向类型',
    vehicle_model  varchar(64)    null comment '车辆品牌',
    longitude      decimal(10, 6) null comment '经度',
    latitude       decimal(10, 6) null comment '纬度',
    flow_direction int default 0  null comment '流向:0-省内,1-入徐,2-出徐'
)
    comment 'ETC通行记录表' collate = utf8mb4_general_ci;
```



```sql
-- auto-generated definition
create table fake_vehicle_alert
(
    id            bigint auto_increment comment '主键'
        primary key,
    plate_number  varchar(20)                           null comment '车牌号',
    start_bayonet varchar(100)                          null comment '起点卡口',
    end_bayonet   varchar(100)                          null comment '终点卡口',
    start_time    datetime                              null comment '起点时间',
    end_time      datetime                              null comment '终点时间(报警时间)',
    distance      decimal(10, 2)                        null comment '距离(km)',
    time_diff     bigint                                null comment '耗时(秒)',
    actual_speed  decimal(10, 2)                        null comment '实际车速(km/h)',
    limit_speed   decimal(10, 2)                        null comment '限速阈值',
    alert_level   varchar(20) default 'HIGH'            null comment '报警等级',
    create_time   datetime    default CURRENT_TIMESTAMP null
)
    comment 'Spark计算的套牌车报警表' collate = utf8mb4_general_ci;


```

建好表之后，启动后端



然后启动数据生成的代码，接下来应该就可以在前端看到对应的内容了
