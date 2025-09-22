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

CREATE SCHEMA product_db;

-- 用户表
CREATE TABLE product_db.users (
                       id INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID，主键自增',
                       username VARCHAR(50) NOT NULL COMMENT '用户名',
                       email VARCHAR(100) NOT NULL COMMENT '用户邮箱',
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '用户注册时间'
) COMMENT='用户表';

-- 商品表
CREATE TABLE product_db.products (
                          id INT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID，主键自增',
                          name VARCHAR(100) NOT NULL COMMENT '商品名称',
                          price DECIMAL(10,2) NOT NULL COMMENT '商品单价',
                          stock INT NOT NULL COMMENT '商品库存数量',
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '商品上架时间'
) COMMENT='商品表';

-- 订单表
CREATE TABLE product_db.orders (
                        id INT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID，主键自增',
                        user_id INT NOT NULL COMMENT '下单用户ID',
                        order_date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
                        total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
                        status VARCHAR(20) DEFAULT 'pending' COMMENT '订单状态（pending/completed/cancelled等）',
                        FOREIGN KEY (user_id) REFERENCES users(id)
) COMMENT='订单表';

-- 订单明细表
CREATE TABLE product_db.order_items (
                             id INT PRIMARY KEY AUTO_INCREMENT COMMENT '订单明细ID，主键自增',
                             order_id INT NOT NULL COMMENT '订单ID',
                             product_id INT NOT NULL COMMENT '商品ID',
                             quantity INT NOT NULL COMMENT '购买数量',
                             unit_price DECIMAL(10,2) NOT NULL COMMENT '下单时商品单价',
                             FOREIGN KEY (order_id) REFERENCES orders(id),
                             FOREIGN KEY (product_id) REFERENCES products(id)
) COMMENT='订单明细表';

-- 商品分类表
CREATE TABLE product_db.categories (
                            id INT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID，主键自增',
                            name VARCHAR(50) NOT NULL COMMENT '分类名称'
) COMMENT='商品分类表';

-- 商品-分类关联表（多对多）
CREATE TABLE product_db.product_categories (
                                    product_id INT NOT NULL COMMENT '商品ID',
                                    category_id INT NOT NULL COMMENT '分类ID',
                                    PRIMARY KEY (product_id, category_id),
                                    FOREIGN KEY (product_id) REFERENCES products(id),
                                    FOREIGN KEY (category_id) REFERENCES categories(id)
) COMMENT='商品与分类关联表';

