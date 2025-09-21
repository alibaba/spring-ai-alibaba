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

CREATE DATABASE product_db;

USE product_db;

-- 用户表
CREATE TABLE users (
                       id INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID，主键自增',
                       username VARCHAR(50) NOT NULL COMMENT '用户名',
                       email VARCHAR(100) NOT NULL COMMENT '用户邮箱',
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '用户注册时间'
) COMMENT='用户表';

-- 商品表
CREATE TABLE products (
                          id INT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID，主键自增',
                          name VARCHAR(100) NOT NULL COMMENT '商品名称',
                          price DECIMAL(10,2) NOT NULL COMMENT '商品单价',
                          stock INT NOT NULL COMMENT '商品库存数量',
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '商品上架时间'
) COMMENT='商品表';

-- 订单表
CREATE TABLE orders (
                        id INT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID，主键自增',
                        user_id INT NOT NULL COMMENT '下单用户ID',
                        order_date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
                        total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
                        status VARCHAR(20) DEFAULT 'pending' COMMENT '订单状态（pending/completed/cancelled等）',
                        FOREIGN KEY (user_id) REFERENCES users(id)
) COMMENT='订单表';

-- 订单明细表
CREATE TABLE order_items (
                             id INT PRIMARY KEY AUTO_INCREMENT COMMENT '订单明细ID，主键自增',
                             order_id INT NOT NULL COMMENT '订单ID',
                             product_id INT NOT NULL COMMENT '商品ID',
                             quantity INT NOT NULL COMMENT '购买数量',
                             unit_price DECIMAL(10,2) NOT NULL COMMENT '下单时商品单价',
                             FOREIGN KEY (order_id) REFERENCES orders(id),
                             FOREIGN KEY (product_id) REFERENCES products(id)
) COMMENT='订单明细表';

-- 商品分类表
CREATE TABLE categories (
                            id INT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID，主键自增',
                            name VARCHAR(50) NOT NULL COMMENT '分类名称'
) COMMENT='商品分类表';

-- 商品-分类关联表（多对多）
CREATE TABLE product_categories (
                                    product_id INT NOT NULL COMMENT '商品ID',
                                    category_id INT NOT NULL COMMENT '分类ID',
                                    PRIMARY KEY (product_id, category_id),
                                    FOREIGN KEY (product_id) REFERENCES products(id),
                                    FOREIGN KEY (category_id) REFERENCES categories(id)
) COMMENT='商品与分类关联表';


-- 插入用户数据
INSERT INTO users (username, email) VALUES
                                        ('alice', 'alice@example.com'),
                                        ('bob', 'bob@example.com'),
                                        ('cathy', 'cathy@example.com'),
                                        ('daniel', 'daniel@example.com'),
                                        ('emily', 'emily@example.com')
    ON DUPLICATE KEY UPDATE username=VALUES(username);

-- 插入商品分类数据
INSERT INTO categories (name) VALUES
                                  ('电子产品'),
                                  ('服装'),
                                  ('图书'),
                                  ('家居用品'),
                                  ('食品');

-- 插入商品数据
INSERT INTO products (name, price, stock) VALUES
                                              ('智能手机', 2999.00, 100),
                                              ('T恤衫', 89.00, 500),
                                              ('小说', 39.00, 200),
                                              ('咖啡机', 599.00, 50),
                                              ('牛奶', 15.00, 300),
                                              ('笔记本电脑', 4999.00, 30),
                                              ('沙发', 2599.00, 10),
                                              ('巧克力', 25.00, 100),
                                              ('羽绒服', 399.00, 80),
                                              ('历史书', 69.00, 150);

-- 插入商品-分类关联数据
INSERT INTO product_categories (product_id, category_id) VALUES
                                                             (1, 1), -- 智能手机-电子产品
                                                             (2, 2), -- T恤衫-服装
                                                             (3, 3), -- 小说-图书
                                                             (4, 1), (4, 4), -- 咖啡机-电子产品、家居用品
                                                             (5, 5), -- 牛奶-食品
                                                             (6, 1), -- 笔记本电脑-电子产品
                                                             (7, 4), -- 沙发-家居用品
                                                             (8, 5), -- 巧克力-食品
                                                             (9, 2), -- 羽绒服-服装
                                                             (10, 3); -- 历史书-图书

-- 插入订单数据
INSERT INTO orders (user_id, total_amount, status, order_date) VALUES
                                                                   (1, 3088.00, 'completed', '2025-06-01 10:10:00'),
                                                                   (2, 39.00, 'pending', '2025-06-02 09:23:00'),
                                                                   (3, 1204.00, 'completed', '2025-06-03 13:45:00'),
                                                                   (4, 65.00, 'cancelled', '2025-06-04 16:05:00'),
                                                                   (5, 5113.00, 'completed', '2025-06-05 20:12:00'),
                                                                   (1, 814.00, 'completed', '2025-06-05 21:03:00'),
                                                                   (2, 424.00, 'pending', '2025-06-06 08:10:00'),
                                                                   (3, 524.00, 'completed', '2025-06-06 14:48:00'),
                                                                   (4, 399.00, 'completed', '2025-06-07 10:15:00'),
                                                                   (5, 129.00, 'pending', '2025-06-07 18:00:00');

-- 插入订单明细数据
INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES
                                                                         (1, 1, 1, 2999.00),
                                                                         (1, 2, 1, 89.00),
                                                                         (2, 3, 1, 39.00),
                                                                         (3, 4, 2, 599.00),
                                                                         (3, 5, 2, 3.00),
                                                                         (4, 8, 2, 25.00),
                                                                         (4, 5, 1, 15.00),
                                                                         (5, 6, 1, 4999.00),
                                                                         (5, 2, 1, 89.00),
                                                                         (5, 5, 5, 5.00),
                                                                         (5, 8, 1, 25.00),
                                                                         (6, 9, 2, 399.00),
                                                                         (6, 3, 1, 16.00),
                                                                         (7, 2, 2, 89.00),
                                                                         (7, 3, 3, 39.00),
                                                                         (8, 10, 4, 69.00),
                                                                         (9, 9, 1, 399.00),
                                                                         (10, 8, 4, 25.00),
                                                                         (10, 5, 1, 29.00);
