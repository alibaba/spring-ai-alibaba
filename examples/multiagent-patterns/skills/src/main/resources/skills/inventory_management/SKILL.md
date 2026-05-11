---
name: inventory_management
description: Database schema and business logic for inventory tracking including products, warehouses, and stock levels.
---

# Inventory Management Schema

## Tables

### products
- product_id (PRIMARY KEY)
- product_name
- sku
- category
- unit_cost
- reorder_point (minimum stock level before reordering)
- discontinued (boolean)

### warehouses
- warehouse_id (PRIMARY KEY)
- warehouse_name
- location
- capacity

### inventory
- inventory_id (PRIMARY KEY)
- product_id (FOREIGN KEY -> products)
- warehouse_id (FOREIGN KEY -> warehouses)
- quantity_on_hand
- last_updated

### stock_movements
- movement_id (PRIMARY KEY)
- product_id (FOREIGN KEY -> products)
- warehouse_id (FOREIGN KEY -> warehouses)
- movement_type (inbound/outbound/transfer/adjustment)
- quantity (positive for inbound, negative for outbound)
- movement_date
- reference_number

## Business Logic

**Available stock**: quantity_on_hand from inventory where quantity_on_hand > 0

**Products needing reorder**: total quantity_on_hand across warehouses <= product's reorder_point

**Active products only**: Exclude discontinued = true unless analyzing discontinued items

## Example Query

```sql
-- Find products below reorder point
SELECT p.product_id, p.product_name, p.reorder_point, SUM(i.quantity_on_hand) as total_stock
FROM products p
JOIN inventory i ON p.product_id = i.product_id
WHERE p.discontinued = false
GROUP BY p.product_id, p.product_name, p.reorder_point
HAVING SUM(i.quantity_on_hand) <= p.reorder_point;
```
