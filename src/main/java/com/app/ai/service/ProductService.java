package com.app.ai.service;

import com.app.ai.model.Product;
import com.app.ai.model.ProductPhoto;
import com.app.ai.model.StockHistory;
import com.app.ai.repository.ProductRepository;
import com.app.ai.repository.ProductPhotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPhotoRepository productPhotoRepository;

    @Autowired
    private StockHistoryService stockHistoryService;

    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> findProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product saveProduct(Product product) {
        // Check if this is an update with stock change
        if (product.getId() != null) {
            Optional<Product> existingProductOpt = productRepository.findById(product.getId());
            if (existingProductOpt.isPresent()) {
                Product existingProduct = existingProductOpt.get();
                Integer oldQuantity = existingProduct.getQuantity();
                Integer newQuantity = product.getQuantity();

                // If quantity changed, record the stock history
                if (!oldQuantity.equals(newQuantity)) {
                    Product saved = productRepository.save(product);
                    productRepository.flush();

                    // Determine change type and reason
                    StockHistory.ChangeType changeType;
                    String reason;

                    if (newQuantity > oldQuantity) {
                        changeType = StockHistory.ChangeType.INVENTORY_RESTOCK;
                        reason = "Stock increased from " + oldQuantity + " to " + newQuantity + " via admin update";
                    } else {
                        changeType = StockHistory.ChangeType.MANUAL_ADJUSTMENT;
                        reason = "Stock decreased from " + oldQuantity + " to " + newQuantity + " via admin update";
                    }

                    stockHistoryService.recordStockChange(saved, oldQuantity, newQuantity, changeType, reason);
                    return saved;
                }
            }
        }

        Product saved = productRepository.save(product);
        productRepository.flush();
        return saved;
    }

    /**
     * Update stock quantity with specific reason and change type
     */
    public Product updateStock(Long productId, Integer newQuantity, StockHistory.ChangeType changeType, String reason) {
        Optional<Product> productOpt = findProductById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            Integer oldQuantity = product.getQuantity();
            product.setQuantity(newQuantity);

            Product saved = productRepository.save(product);

            // Record stock history
            stockHistoryService.recordStockChange(saved, oldQuantity, newQuantity, changeType, reason);

            return saved;
        }
        throw new IllegalArgumentException("Product not found with ID: " + productId);
    }

    /**
     * Reduce stock for order (used by order service)
     */
    public Product reduceStock(Long productId, Integer quantity, Long orderId) {
        Optional<Product> productOpt = findProductById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            Integer oldQuantity = product.getQuantity();

            if (oldQuantity < quantity) {
                throw new IllegalArgumentException("Insufficient stock. Available: " + oldQuantity + ", Requested: " + quantity);
            }

            Integer newQuantity = oldQuantity - quantity;
            product.setQuantity(newQuantity);

            Product saved = productRepository.save(product);

            // Record stock history for order
            String reason = "Stock reduced by " + quantity + " units for order #" + orderId;
            stockHistoryService.recordStockChange(saved, oldQuantity, newQuantity,
                                                StockHistory.ChangeType.ORDER_PURCHASE, reason, orderId);

            return saved;
        }
        throw new IllegalArgumentException("Product not found with ID: " + productId);
    }

    /**
     * Restore stock for order cancellation
     */
    public Product restoreStock(Long productId, Integer quantity, Long orderId) {
        Optional<Product> productOpt = findProductById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            Integer oldQuantity = product.getQuantity();
            Integer newQuantity = oldQuantity + quantity;
            product.setQuantity(newQuantity);

            Product saved = productRepository.save(product);

            // Record stock history for order cancellation
            String reason = "Stock restored by " + quantity + " units due to order #" + orderId + " cancellation";
            stockHistoryService.recordStockChange(saved, oldQuantity, newQuantity,
                                                StockHistory.ChangeType.ORDER_CANCELLATION, reason, orderId);

            return saved;
        }
        throw new IllegalArgumentException("Product not found with ID: " + productId);
    }

    public Page<Product> findPaginatedProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public List<ProductPhoto> getProductPhotos(Product product) {
        return productPhotoRepository.findByProduct(product);
    }

    public ProductPhoto saveProductPhoto(Product product, MultipartFile file) throws IOException {
        // Delete existing photos for this product to replace them
        List<ProductPhoto> existingPhotos = productPhotoRepository.findByProduct(product);
        if (!existingPhotos.isEmpty()) {
            productPhotoRepository.deleteAll(existingPhotos);
        }

        // Save the new photo
        ProductPhoto photo = new ProductPhoto();
        photo.setProduct(product);
        photo.setFileName(file.getOriginalFilename());
        photo.setFileType(file.getContentType());
        photo.setData(file.getBytes());
        return productPhotoRepository.save(photo);
    }

    public Optional<ProductPhoto> getProductPhotoById(Long id) {
        return productPhotoRepository.findById(id);
    }

    public void deleteProductPhoto(Long photoId) {
        productPhotoRepository.deleteById(photoId);
    }

    public void deleteProduct(Long id) {
        // First delete all associated photos
        Optional<Product> productOpt = findProductById(id);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            List<ProductPhoto> photos = getProductPhotos(product);
            if (!photos.isEmpty()) {
                productPhotoRepository.deleteAll(photos);
            }
        }

        // Then delete the product
        productRepository.deleteById(id);
    }
}
