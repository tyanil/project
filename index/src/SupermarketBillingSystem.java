import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

// Main application class that coordinates between the three forms
public class SupermarketBillingSystem extends JFrame {
    // Data structures
    private static ArrayList<Product> products;
    private static ArrayList<Product> cart;
    private static HashMap<String, Double> discounts;
    private static int receiptNumber = 1000;
    private static File receiptDirectory = new File("receipts");

    // The three forms
    private static ProductManagementForm productForm;
    private static ShoppingCartForm cartForm;
    private static CheckoutForm checkoutForm;

    public static void main(String[] args) {
        // Set look and feel to system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize data structures
        products = new ArrayList<>();
        cart = new ArrayList<>();
        discounts = new HashMap<>();

        // Setup discounts
        initializeDiscounts();

        // Setup sample products
        initializeSampleProducts();

        // Create directory for receipts if it doesn't exist
        if (!receiptDirectory.exists()) {
            receiptDirectory.mkdir();
        }

        // Start the application with all three forms
        SwingUtilities.invokeLater(() -> {
            productForm = new ProductManagementForm();
            cartForm = new ShoppingCartForm();
            checkoutForm = new CheckoutForm();

            // Position the windows
            positionWindows();
        });
    }

    private static void positionWindows() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width / 3;

        productForm.setSize(width, 600);
        cartForm.setSize(width, 600);
        checkoutForm.setSize(width, 600);

        productForm.setLocation(0, 100);
        cartForm.setLocation(width, 100);
        checkoutForm.setLocation(width * 2, 100);
    }

    private static void initializeDiscounts() {
        discounts.put("Loyalty Card", 0.05);
        discounts.put("Senior Citizen", 0.10);
        discounts.put("Special Offer", 0.15);
        discounts.put("Festive Season", 0.20);
    }

    private static void initializeSampleProducts() {
        products.add(new Product("P001", "Rice (1kg)", 40.00, 100));
        products.add(new Product("P002", "Milk (1L)", 60.00, 50));
        products.add(new Product("P003", "Bread", 49.00, 30));
        products.add(new Product("P004", "Eggs (12)", 144.00, 40));
        products.add(new Product("P005", "Chicken (1kg)", 189.00, 20));
        products.add(new Product("P006", "Apples (1kg)", 125.00, 30));
        products.add(new Product("P007", "Pasta (500g)", 55.00, 45));
        products.add(new Product("P008", "Tomatoes (1kg)", 119.00, 25));
        products.add(new Product("P009", "Cheese (250g)", 200.00, 15));
        products.add(new Product("P010", "Orange Juice (1L)", 110.00, 35));
    }

    // Get product list
    public static ArrayList<Product> getProducts() {
        return products;
    }

    // Get cart items
    public static ArrayList<Product> getCart() {
        return cart;
    }

    // Get discounts
    public static HashMap<String, Double> getDiscounts() {
        return discounts;
    }

    // Add product to cart
    public static boolean addToCart(String id, int quantity) {
        // Find the product in our product list
        Product selectedProduct = null;
        for (Product product : products) {
            if (product.getId().equals(id)) {
                selectedProduct = product;
                break;
            }
        }

        if (selectedProduct == null) {
            return false;
        }

        if (quantity > selectedProduct.getQuantity()) {
            return false;
        }

        // Check if product already in cart
        boolean found = false;
        for (Product cartProduct : cart) {
            if (cartProduct.getId().equals(id)) {
                cartProduct.setQuantity(cartProduct.getQuantity() + quantity);
                found = true;
                break;
            }
        }

        if (!found) {
            // Add new product to cart
            Product cartProduct = new Product(
                    selectedProduct.getId(),
                    selectedProduct.getName(),
                    selectedProduct.getPrice(),
                    quantity
            );
            cart.add(cartProduct);
        }

        // Update product stock
        selectedProduct.setQuantity(selectedProduct.getQuantity() - quantity);

        // Refresh all forms
        if (productForm != null) productForm.refreshTable();
        if (cartForm != null) cartForm.refreshTable();
        if (checkoutForm != null) checkoutForm.updateTotals();

        return true;
    }

    // Remove product from cart
    public static void removeFromCart(String id, int quantity) {
        // Return quantity to product stock
        for (Product product : products) {
            if (product.getId().equals(id)) {
                product.setQuantity(product.getQuantity() + quantity);
                break;
            }
        }

        // Remove from cart
        for (int i = 0; i < cart.size(); i++) {
            if (cart.get(i).getId().equals(id)) {
                cart.remove(i);
                break;
            }
        }

        // Refresh all forms
        if (productForm != null) productForm.refreshTable();
        if (cartForm != null) cartForm.refreshTable();
        if (checkoutForm != null) checkoutForm.updateTotals();
    }

    // Clear cart and return items to inventory
    public static void clearCart() {
        // Return all items to inventory
        for (Product cartProduct : cart) {
            for (Product product : products) {
                if (product.getId().equals(cartProduct.getId())) {
                    product.setQuantity(product.getQuantity() + cartProduct.getQuantity());
                    break;
                }
            }
        }

        // Clear cart
        cart.clear();

        // Refresh all forms
        if (productForm != null) productForm.refreshTable();
        if (cartForm != null) cartForm.refreshTable();
        if (checkoutForm != null) {
            checkoutForm.resetDiscount();
            checkoutForm.updateTotals();
        }
    }

    // Process checkout and generate receipt
    public static boolean checkout(String discountType) {
        if (cart.isEmpty()) {
            return false;
        }

        double discountRate = 0.0;
        if (!discountType.startsWith("None")) {
            for (Map.Entry<String, Double> entry : discounts.entrySet()) {
                if (discountType.startsWith(entry.getKey())) {
                    discountRate = entry.getValue();
                    break;
                }
            }
        }

        // Generate receipt
        String receiptContent = generateReceipt(discountType, discountRate);

        // Save receipt to file
        String fileName = String.format("receipt_%d.txt", receiptNumber);
        File receiptFile = new File(receiptDirectory, fileName);

        try (PrintWriter writer = new PrintWriter(receiptFile)) {
            writer.print(receiptContent);
            JOptionPane.showMessageDialog(null,
                    "Checkout successful!\nReceipt saved as: " + receiptFile.getAbsolutePath());

            // Increment receipt number for next bill
            receiptNumber++;

            // Clear cart for next customer
            cart.clear();

            // Refresh all forms
            if (productForm != null) productForm.refreshTable();
            if (cartForm != null) cartForm.refreshTable();
            if (checkoutForm != null) {
                checkoutForm.resetDiscount();
                checkoutForm.updateTotals();
            }

            return true;

        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(null,
                    "Error saving receipt: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // Calculate total without discount
    public static double calculateTotal() {
        double total = 0;
        for (Product product : cart) {
            total += product.getPrice() * product.getQuantity();
        }
        return total;
    }

    // Generate receipt content
    private static String generateReceipt(String discountName, double discountRate) {
        StringBuilder receipt = new StringBuilder();

        // Header
        receipt.append("====================================\n");
        receipt.append("          SUPERMARKET RECEIPT        \n");
        receipt.append("====================================\n\n");

        // Receipt details
        receipt.append("Receipt #: ").append(receiptNumber).append("\n");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        receipt.append("Date: ").append(dateFormat.format(new Date())).append("\n\n");

        // Items
        receipt.append("ITEMS:\n");
        receipt.append(String.format("%-5s %-20s %-10s %-10s %-10s\n",
                "ID", "Product", "Price", "Qty", "Total"));
        receipt.append("------------------------------------------------------------\n");

        for (Product product : cart) {
            double total = product.getPrice() * product.getQuantity();
            receipt.append(String.format("%-5s %-20s ₱%-9.2f %-10d ₱%-9.2f\n",
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    product.getQuantity(),
                    total));
        }

        receipt.append("------------------------------------------------------------\n\n");

        // Totals
        double total = calculateTotal();
        double discountAmount = total * discountRate;
        double discountedTotal = total - discountAmount;

        receipt.append(String.format("Subtotal:          ₱%.2f\n", total));

        // Discount info
        if (!discountName.startsWith("None")) {
            String discountType = discountName.split("\\(")[0].trim();
            receipt.append(String.format("Discount (%s): -₱%.2f\n",
                    discountType, discountAmount));
        }

        receipt.append(String.format("TOTAL:             ₱%.2f\n\n", discountedTotal));

        // Footer
        receipt.append("====================================\n");
        receipt.append("         Thank you for shopping!    \n");
        receipt.append("====================================\n");

        return receipt.toString();
    }
}