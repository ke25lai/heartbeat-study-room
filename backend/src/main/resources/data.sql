-- 班级种子数据（排行榜按班级分组可配置）
-- 用 INSERT IGNORE 保证重复启动不报错（MySQL 与 H2 MySQL 模式均支持）
INSERT IGNORE INTO class_group (id, name) VALUES (1, '计算机科学与技术1班');
INSERT IGNORE INTO class_group (id, name) VALUES (2, '计算机科学与技术2班');
INSERT IGNORE INTO class_group (id, name) VALUES (3, '软件工程1班');
