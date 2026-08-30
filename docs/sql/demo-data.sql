-- 演示数据：近7日订单（仅用于仪表板展示，生产环境请勿执行）
USE fruit_mall;
INSERT INTO `order` (order_no,user_id,status,total_amount,pay_amount,freight,address_snapshot,delivery_type,remark,paid_at,created_at)
VALUES ('FM26087007',1,40,7980,7980,0,'{\"receiver\":\"张三\",\"phone\":\"13800001111\",\"province\":\"广东省\",\"city\":\"深圳市\",\"district\":\"南山区\",\"detail\":\"科技园南路88号\"}',1,'演示数据',
DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 39960 SECOND,
DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 39660 SECOND);
SET @o1002 = LAST_INSERT_ID();
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1002,1,1,'阿克苏冰糖心苹果','5斤装','/images/products/aksu-apple.svg',3990,2,7980);
INSERT INTO payment (order_no,transaction_id,channel,amount,status,paid_at)
VALUES ('FM26087007','MOCK-DEMO-1002','MOCK',7980,1,DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 39960 SECOND);
INSERT INTO `order` (order_no,user_id,status,total_amount,pay_amount,freight,address_snapshot,delivery_type,remark,paid_at,created_at)
VALUES ('FM26087014',1,30,17890,17890,0,'{\"receiver\":\"张三\",\"phone\":\"13800001111\",\"province\":\"广东省\",\"city\":\"深圳市\",\"district\":\"南山区\",\"detail\":\"科技园南路88号\"}',1,'演示数据',
DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 44340 SECOND,
DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 44040 SECOND);
SET @o1003 = LAST_INSERT_ID();
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1003,2,3,'赣南脐橙','9斤装','/images/products/gannan-orange.svg',4990,1,4990);
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1003,5,7,'智利车厘子 JJ级','2斤装','/images/products/chile-cherry.svg',12900,1,12900);
INSERT INTO payment (order_no,transaction_id,channel,amount,status,paid_at)
VALUES ('FM26087014','MOCK-DEMO-1003','MOCK',17890,1,DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 44340 SECOND);
INSERT INTO `order` (order_no,user_id,status,total_amount,pay_amount,freight,address_snapshot,delivery_type,remark,paid_at,created_at)
VALUES ('FM26087021',1,40,7980,7980,0,'{\"receiver\":\"张三\",\"phone\":\"13800001111\",\"province\":\"广东省\",\"city\":\"深圳市\",\"district\":\"南山区\",\"detail\":\"科技园南路88号\"}',1,'演示数据',
DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 48720 SECOND,
DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 48420 SECOND);
SET @o1004 = LAST_INSERT_ID();
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1004,3,4,'丹东红颜草莓','1盒约1斤','/images/products/dandong-strawberry.svg',3990,2,7980);
INSERT INTO payment (order_no,transaction_id,channel,amount,status,paid_at)
VALUES ('FM26087021','MOCK-DEMO-1004','MOCK',7980,1,DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 48720 SECOND);
INSERT INTO `order` (order_no,user_id,status,total_amount,pay_amount,freight,address_snapshot,delivery_type,remark,paid_at,created_at)
VALUES ('FM26087028',1,40,4990,4990,0,'{\"receiver\":\"张三\",\"phone\":\"13800001111\",\"province\":\"广东省\",\"city\":\"深圳市\",\"district\":\"南山区\",\"detail\":\"科技园南路88号\"}',1,'演示数据',
DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 53100 SECOND,
DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 52800 SECOND);
SET @o1005 = LAST_INSERT_ID();
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1005,4,6,'云南高山蓝莓','125g×4盒','/images/products/yunnan-blueberry.svg',4990,1,4990);
INSERT INTO payment (order_no,transaction_id,channel,amount,status,paid_at)
VALUES ('FM26087028','MOCK-DEMO-1005','MOCK',4990,1,DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 53100 SECOND);
INSERT INTO `order` (order_no,user_id,status,total_amount,pay_amount,freight,address_snapshot,delivery_type,remark,paid_at,created_at)
VALUES ('FM26087035',1,40,33780,33780,0,'{\"receiver\":\"张三\",\"phone\":\"13800001111\",\"province\":\"广东省\",\"city\":\"深圳市\",\"district\":\"南山区\",\"detail\":\"科技园南路88号\"}',1,'演示数据',
DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 57480 SECOND,
DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 57180 SECOND);
SET @o1006 = LAST_INSERT_ID();
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1006,5,7,'智利车厘子 JJ级','2斤装','/images/products/chile-cherry.svg',12900,2,25800);
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1006,1,1,'阿克苏冰糖心苹果','5斤装','/images/products/aksu-apple.svg',3990,2,7980);
INSERT INTO payment (order_no,transaction_id,channel,amount,status,paid_at)
VALUES ('FM26087035','MOCK-DEMO-1006','MOCK',33780,1,DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 57480 SECOND);
INSERT INTO `order` (order_no,user_id,status,total_amount,pay_amount,freight,address_snapshot,delivery_type,remark,paid_at,created_at)
VALUES ('FM26087042',1,40,9900,9900,0,'{\"receiver\":\"张三\",\"phone\":\"13800001111\",\"province\":\"广东省\",\"city\":\"深圳市\",\"district\":\"南山区\",\"detail\":\"科技园南路88号\"}',1,'演示数据',
DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 58260 SECOND,
DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 57960 SECOND);
SET @o1007 = LAST_INSERT_ID();
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1007,6,9,'泰国金枕头榴莲','4-5斤整果','/images/products/thai-durian.svg',9900,1,9900);
INSERT INTO payment (order_no,transaction_id,channel,amount,status,paid_at)
VALUES ('FM26087042','MOCK-DEMO-1007','MOCK',9900,1,DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 58260 SECOND);
INSERT INTO `order` (order_no,user_id,status,total_amount,pay_amount,freight,address_snapshot,delivery_type,remark,paid_at,created_at)
VALUES ('FM26087049',1,40,39800,39800,0,'{\"receiver\":\"张三\",\"phone\":\"13800001111\",\"province\":\"广东省\",\"city\":\"深圳市\",\"district\":\"南山区\",\"detail\":\"科技园南路88号\"}',1,'演示数据',
DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 62640 SECOND,
DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 62340 SECOND);
SET @o1008 = LAST_INSERT_ID();
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1008,7,10,'四季鲜果礼盒','十二层礼盒','/images/products/gift-box.svg',19900,2,39800);
INSERT INTO payment (order_no,transaction_id,channel,amount,status,paid_at)
VALUES ('FM26087049','MOCK-DEMO-1008','MOCK',39800,1,DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 62640 SECOND);
INSERT INTO `order` (order_no,user_id,status,total_amount,pay_amount,freight,address_snapshot,delivery_type,remark,paid_at,created_at)
VALUES ('FM26087056',1,30,8980,8980,0,'{\"receiver\":\"张三\",\"phone\":\"13800001111\",\"province\":\"广东省\",\"city\":\"深圳市\",\"district\":\"南山区\",\"detail\":\"科技园南路88号\"}',1,'演示数据',
DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 67020 SECOND,
DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 66720 SECOND);
SET @o1009 = LAST_INSERT_ID();
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1009,1,1,'阿克苏冰糖心苹果','5斤装','/images/products/aksu-apple.svg',3990,1,3990);
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1009,4,6,'云南高山蓝莓','125g×4盒','/images/products/yunnan-blueberry.svg',4990,1,4990);
INSERT INTO payment (order_no,transaction_id,channel,amount,status,paid_at)
VALUES ('FM26087056','MOCK-DEMO-1009','MOCK',8980,1,DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 67020 SECOND);
INSERT INTO `order` (order_no,user_id,status,total_amount,pay_amount,freight,address_snapshot,delivery_type,remark,paid_at,created_at)
VALUES ('FM26087063',1,40,9980,9980,0,'{\"receiver\":\"张三\",\"phone\":\"13800001111\",\"province\":\"广东省\",\"city\":\"深圳市\",\"district\":\"南山区\",\"detail\":\"科技园南路88号\"}',1,'演示数据',
DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 35400 SECOND,
DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 35100 SECOND);
SET @o1010 = LAST_INSERT_ID();
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1010,2,3,'赣南脐橙','9斤装','/images/products/gannan-orange.svg',4990,2,9980);
INSERT INTO payment (order_no,transaction_id,channel,amount,status,paid_at)
VALUES ('FM26087063','MOCK-DEMO-1010','MOCK',9980,1,DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 35400 SECOND);
INSERT INTO `order` (order_no,user_id,status,total_amount,pay_amount,freight,address_snapshot,delivery_type,remark,paid_at,created_at)
VALUES ('FM26087070',1,40,3990,3990,0,'{\"receiver\":\"张三\",\"phone\":\"13800001111\",\"province\":\"广东省\",\"city\":\"深圳市\",\"district\":\"南山区\",\"detail\":\"科技园南路88号\"}',1,'演示数据',
DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 36180 SECOND,
DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 36000 SECOND);
SET @o1011 = LAST_INSERT_ID();
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1011,3,4,'丹东红颜草莓','1盒约1斤','/images/products/dandong-strawberry.svg',3990,1,3990);
INSERT INTO payment (order_no,transaction_id,channel,amount,status,paid_at)
VALUES ('FM26087070','MOCK-DEMO-1011','MOCK',3990,1,DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 36180 SECOND);
INSERT INTO `order` (order_no,user_id,status,total_amount,pay_amount,freight,address_snapshot,delivery_type,remark,paid_at,created_at)
VALUES ('FM26087077',1,40,49780,49780,0,'{\"receiver\":\"张三\",\"phone\":\"13800001111\",\"province\":\"广东省\",\"city\":\"深圳市\",\"district\":\"南山区\",\"detail\":\"科技园南路88号\"}',1,'演示数据',
DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 40560 SECOND,
DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 40260 SECOND);
SET @o1012 = LAST_INSERT_ID();
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1012,4,6,'云南高山蓝莓','125g×4盒','/images/products/yunnan-blueberry.svg',4990,2,9980);
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1012,7,10,'四季鲜果礼盒','十二层礼盒','/images/products/gift-box.svg',19900,2,39800);
INSERT INTO payment (order_no,transaction_id,channel,amount,status,paid_at)
VALUES ('FM26087077','MOCK-DEMO-1012','MOCK',49780,1,DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 40560 SECOND);
INSERT INTO `order` (order_no,user_id,status,total_amount,pay_amount,freight,address_snapshot,delivery_type,remark,paid_at,created_at)
VALUES ('FM26087084',1,40,12900,12900,0,'{\"receiver\":\"张三\",\"phone\":\"13800001111\",\"province\":\"广东省\",\"city\":\"深圳市\",\"district\":\"南山区\",\"detail\":\"科技园南路88号\"}',1,'演示数据',
DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 44940 SECOND,
DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 44640 SECOND);
SET @o1013 = LAST_INSERT_ID();
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1013,5,7,'智利车厘子 JJ级','2斤装','/images/products/chile-cherry.svg',12900,1,12900);
INSERT INTO payment (order_no,transaction_id,channel,amount,status,paid_at)
VALUES ('FM26087084','MOCK-DEMO-1013','MOCK',12900,1,DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 44940 SECOND);
INSERT INTO `order` (order_no,user_id,status,total_amount,pay_amount,freight,address_snapshot,delivery_type,remark,paid_at,created_at)
VALUES ('FM26087091',1,20,19800,19800,0,'{\"receiver\":\"张三\",\"phone\":\"13800001111\",\"province\":\"广东省\",\"city\":\"深圳市\",\"district\":\"南山区\",\"detail\":\"科技园南路88号\"}',1,'演示数据',
DATE_SUB(NOW(), INTERVAL 0 DAY) + INTERVAL 49320 SECOND,
DATE_SUB(NOW(), INTERVAL 0 DAY) + INTERVAL 49020 SECOND);
SET @o1014 = LAST_INSERT_ID();
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1014,6,9,'泰国金枕头榴莲','4-5斤整果','/images/products/thai-durian.svg',9900,2,19800);
INSERT INTO payment (order_no,transaction_id,channel,amount,status,paid_at)
VALUES ('FM26087091','MOCK-DEMO-1014','MOCK',19800,1,DATE_SUB(NOW(), INTERVAL 0 DAY) + INTERVAL 49320 SECOND);
INSERT INTO `order` (order_no,user_id,status,total_amount,pay_amount,freight,address_snapshot,delivery_type,remark,paid_at,created_at)
VALUES ('FM26087098',1,40,23890,23890,0,'{\"receiver\":\"张三\",\"phone\":\"13800001111\",\"province\":\"广东省\",\"city\":\"深圳市\",\"district\":\"南山区\",\"detail\":\"科技园南路88号\"}',1,'演示数据',
DATE_SUB(NOW(), INTERVAL 0 DAY) + INTERVAL 53700 SECOND,
DATE_SUB(NOW(), INTERVAL 0 DAY) + INTERVAL 53400 SECOND);
SET @o1015 = LAST_INSERT_ID();
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1015,7,10,'四季鲜果礼盒','十二层礼盒','/images/products/gift-box.svg',19900,1,19900);
INSERT INTO order_item (order_id,product_id,sku_id,product_name,sku_spec,image,price,quantity,subtotal)
VALUES (@o1015,3,4,'丹东红颜草莓','1盒约1斤','/images/products/dandong-strawberry.svg',3990,1,3990);
INSERT INTO payment (order_no,transaction_id,channel,amount,status,paid_at)
VALUES ('FM26087098','MOCK-DEMO-1015','MOCK',23890,1,DATE_SUB(NOW(), INTERVAL 0 DAY) + INTERVAL 53700 SECOND);
