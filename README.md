# Portfolio Performance Tracker  

A program designed to provide an overview of your total return across all stock investments, including past transactions, not just your current holdings.  

Keep track of your returns even when buying and selling multiple times over time.  

## How to Use  

### 1. Prepare Your CSV File  

- Download your transaction history from your bank as a CSV file.  
- The file **must** be named `transactions.csv`, or you can edit line 15 in `Konto.java` to change the filename.  
- The file **must** use `;` (semicolon) as a separator.  
- The `"Verdipapir"` field must contain the **ticker symbol**, not the full stock name.  

### 2. Configure Sorting  

- In `Konto.java`, update the `int sorteringsKriterium` variable to define how `oversikt.txt` should be sorted.  

