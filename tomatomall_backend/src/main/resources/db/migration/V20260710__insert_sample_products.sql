USE `tomatomall`;

ALTER TABLE `products` MODIFY COLUMN `title` VARCHAR(100) NOT NULL;

-- ==================== 书籍类商品 ====================

-- 1. 《深入理解计算机系统》
INSERT INTO `products` (`title`, `price`, `rate`, `description`, `cover`, `detail`) VALUES
('深入理解计算机系统', 129.00, 4.9, '从程序员的视角详细阐述计算机系统的本质概念，包括程序如何映射到系统上，程序如何执行，以及系统如何优化', 
'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=computer%20systems%20textbook%20cover%20blue%20tech%20style&image_size=square', 
'本书从程序员的角度详细阐述计算机系统的本质概念，包括程序如何映射到系统上，程序如何执行，以及系统如何优化。全书共12章，分为五个部分');

SET @pid1 = LAST_INSERT_ID();
INSERT INTO `stockpiles` (`product_id`, `amount`, `frozen`, `version`) VALUES
(@pid1, 500, 0, 1);

INSERT INTO `specifications` (`product_id`, `item`, `value`) VALUES
(@pid1, '作者', 'Randal E. Bryant'),
(@pid1, '出版社', '机械工业出版社'),
(@pid1, 'ISBN', '9787111544937'),
(@pid1, '页数', '752'),
(@pid1, '装帧', '平装');

-- 2. 《分布式系统：概念与设计》
INSERT INTO `products` (`title`, `price`, `rate`, `description`, `cover`, `detail`) VALUES
('分布式系统：概念与设计', 89.00, 4.7, '分布式系统领域的经典教材，全面介绍分布式系统的设计原理和实践方法', 
'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=distributed%20systems%20textbook%20cover%20network%20cloud%20style&image_size=square', 
'本书是分布式系统领域的权威教材，全面介绍分布式系统的设计原理、算法和实践经验');

SET @pid2 = LAST_INSERT_ID();
INSERT INTO `stockpiles` (`product_id`, `amount`, `frozen`, `version`) VALUES
(@pid2, 300, 0, 1);

INSERT INTO `specifications` (`product_id`, `item`, `value`) VALUES
(@pid2, '作者', 'George Coulouris'),
(@pid2, '出版社', '机械工业出版社'),
(@pid2, 'ISBN', '9787111612753'),
(@pid2, '页数', '688'),
(@pid2, '装帧', '精装');

-- 3. 《Java编程思想》
INSERT INTO `products` (`title`, `price`, `rate`, `description`, `cover`, `detail`) VALUES
('Java编程思想', 108.00, 4.8, 'Java学习必读经典，全面深入地介绍Java语言的核心概念和高级特性', 
'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=java%20programming%20textbook%20cover%20coffee%20cup%20style&image_size=square', 
'本书是学习Java编程的必读经典，作者Bruce Eckel以通俗易懂的方式讲解了Java语言的核心概念');

SET @pid3 = LAST_INSERT_ID();
INSERT INTO `stockpiles` (`product_id`, `amount`, `frozen`, `version`) VALUES
(@pid3, 800, 0, 1);

INSERT INTO `specifications` (`product_id`, `item`, `value`) VALUES
(@pid3, '作者', 'Bruce Eckel'),
(@pid3, '出版社', '机械工业出版社'),
(@pid3, 'ISBN', '9787111213826'),
(@pid3, '页数', '1000'),
(@pid3, '装帧', '平装');

-- ==================== 电子产品类商品 ====================

-- 4. AirPods Pro 2
INSERT INTO `products` (`title`, `price`, `rate`, `description`, `cover`, `detail`) VALUES
('AirPods Pro 2', 1899.00, 4.8, 'Apple旗舰级真无线降噪耳机，自适应降噪，个性化空间音频', 
'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=wireless%20earbuds%20white%20modern%20tech%20product%20photo&image_size=square', 
'AirPods Pro第二代带来更出色的自适应降噪功能，配合个性化空间音频，为你带来沉浸式聆听体验');

SET @pid4 = LAST_INSERT_ID();
INSERT INTO `stockpiles` (`product_id`, `amount`, `frozen`, `version`) VALUES
(@pid4, 200, 0, 1);

INSERT INTO `specifications` (`product_id`, `item`, `value`) VALUES
(@pid4, '品牌', 'Apple'),
(@pid4, '型号', 'AirPods Pro 2'),
(@pid4, '降噪', '自适应主动降噪'),
(@pid4, '续航', '6小时'),
(@pid4, '充电盒续航', '30小时');

-- 5. Apple Watch Series 9
INSERT INTO `products` (`title`, `price`, `rate`, `description`, `cover`, `detail`) VALUES
('Apple Watch Series 9', 3199.00, 4.9, '最新款苹果智能手表，S9芯片，双指轻点手势，更亮屏幕', 
'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=smart%20watch%20black%20modern%20tech%20product%20photo&image_size=square', 
'Apple Watch Series 9配备全新S9芯片，带来更强大的性能和全新的双指轻点手势');

SET @pid5 = LAST_INSERT_ID();
INSERT INTO `stockpiles` (`product_id`, `amount`, `frozen`, `version`) VALUES
(@pid5, 150, 0, 1);

INSERT INTO `specifications` (`product_id`, `item`, `value`) VALUES
(@pid5, '品牌', 'Apple'),
(@pid5, '型号', 'Apple Watch Series 9'),
(@pid5, '屏幕尺寸', '41mm'),
(@pid5, '处理器', 'S9'),
(@pid5, '防水', '50米');

-- 6. HHKB 静电容键盘
INSERT INTO `products` (`title`, `price`, `rate`, `description`, `cover`, `detail`) VALUES
('HHKB Professional HYBRID', 2699.00, 4.9, '程序员梦寐以求的静电容键盘，蓝牙双模，45g静音轴', 
'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=mechanical%20keyboard%20black%20minimalist%20product%20photo&image_size=square', 
'HHKB是程序员的终极键盘选择，静电容开关提供无与伦比的打字体验');

SET @pid6 = LAST_INSERT_ID();
INSERT INTO `stockpiles` (`product_id`, `amount`, `frozen`, `version`) VALUES
(@pid6, 80, 0, 1);

INSERT INTO `specifications` (`product_id`, `item`, `value`) VALUES
(@pid6, '品牌', 'Happy Hacking'),
(@pid6, '型号', 'HHKB Professional HYBRID'),
(@pid6, '轴体', '静电容45g'),
(@pid6, '连接方式', '蓝牙/USB-C'),
(@pid6, '配列', '60%');

-- ==================== 服装类商品 ====================

-- 7. 优衣库纯棉T恤
INSERT INTO `products` (`title`, `price`, `rate`, `description`, `cover`, `detail`) VALUES
('优衣库纯棉圆领T恤', 79.00, 4.6, '经典基础款纯棉T恤，舒适透气，百搭单品', 
'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=cotton%20tshirt%20white%20minimalist%20product%20photo&image_size=square', 
'采用优质纯棉面料，柔软舒适，四季皆宜的经典基础款');

SET @pid7 = LAST_INSERT_ID();
INSERT INTO `stockpiles` (`product_id`, `amount`, `frozen`, `version`) VALUES
(@pid7, 1000, 0, 1);

INSERT INTO `specifications` (`product_id`, `item`, `value`) VALUES
(@pid7, '品牌', 'UNIQLO'),
(@pid7, '材质', '100%纯棉'),
(@pid7, '颜色', '白色/黑色/灰色'),
(@pid7, '尺码', 'S/M/L/XL/XXL'),
(@pid7, '版型', '标准款');

-- 8. Levi's 501牛仔裤
INSERT INTO `products` (`title`, `price`, `rate`, `description`, `cover`, `detail`) VALUES
('Levi''s 501经典牛仔裤', 599.00, 4.7, '经典直筒版型，百年工艺，百搭时尚', 
'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=jeans%20blue%20classic%20product%20photo&image_size=square', 
'Levi''s经典501牛仔裤，直筒版型适合各种身材，经典靛蓝水洗');

SET @pid8 = LAST_INSERT_ID();
INSERT INTO `stockpiles` (`product_id`, `amount`, `frozen`, `version`) VALUES
(@pid8, 400, 0, 1);

INSERT INTO `specifications` (`product_id`, `item`, `value`) VALUES
(@pid8, '品牌', 'Levi''s'),
(@pid8, '型号', '501'),
(@pid8, '材质', '牛仔布'),
(@pid8, '颜色', '靛蓝'),
(@pid8, '版型', '直筒');

-- ==================== 数码配件类商品 ====================

-- 9. Anker USB-C数据线
INSERT INTO `products` (`title`, `price`, `rate`, `description`, `cover`, `detail`) VALUES
('Anker USB-C快充线', 39.00, 4.8, '100W快充支持，编织线材耐用，1.8米长度', 
'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=usb%20c%20cable%20white%20braided%20product%20photo&image_size=square', 
'Anker高品质编织数据线，支持100W快充，耐用不易断裂');

SET @pid9 = LAST_INSERT_ID();
INSERT INTO `stockpiles` (`product_id`, `amount`, `frozen`, `version`) VALUES
(@pid9, 2000, 0, 1);

INSERT INTO `specifications` (`product_id`, `item`, `value`) VALUES
(@pid9, '品牌', 'Anker'),
(@pid9, '接口', 'USB-C to USB-C'),
(@pid9, '功率', '100W'),
(@pid9, '长度', '1.8米'),
(@pid9, '材质', '尼龙编织');

-- 10. 小米移动电源
INSERT INTO `products` (`title`, `price`, `rate`, `description`, `cover`, `detail`) VALUES
('小米移动电源3 20000mAh', 199.00, 4.7, '大容量20000mAh，支持45W快充，三设备同时充电', 
'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=power%20bank%20white%20minimalist%20product%20photo&image_size=square', 
'小米经典移动电源，大容量支持，多设备快充');

SET @pid10 = LAST_INSERT_ID();
INSERT INTO `stockpiles` (`product_id`, `amount`, `frozen`, `version`) VALUES
(@pid10, 600, 0, 1);

INSERT INTO `specifications` (`product_id`, `item`, `value`) VALUES
(@pid10, '品牌', '小米'),
(@pid10, '容量', '20000mAh'),
(@pid10, '最大输出', '45W'),
(@pid10, '接口', 'USB-C x2, USB-A x1'),
(@pid10, '重量', '390g');

SELECT '数据插入完成' as result;
