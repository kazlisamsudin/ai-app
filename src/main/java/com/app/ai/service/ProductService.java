package com.app.ai.service;

import com.app.ai.model.Product;
import com.app.ai.model.ProductPhoto;
import com.app.ai.repository.ProductPhotoRepository;
import com.app.ai.repository.ProductRepository;
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

    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> findProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product saveProduct(Product product) {
        Product saved = productRepository.save(product);
        productRepository.flush(); // Ensure ID is generated and persisted for photo
        return saved;
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
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
}
