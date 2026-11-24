package com.stylehub.service;

import com.stylehub.exception.InsufficientStockException;
import com.stylehub.exception.ResourceNotFoundException;
import com.stylehub.model.*;
import com.stylehub.repo.CartItemRepository;
import com.stylehub.repo.CartRepository;
import com.stylehub.repo.ComboPackageRepository;
import com.stylehub.repo.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final ProductRepository productRepo;
    private final ComboPackageRepository comboRepo;
    private final UserService userService;

    public CartService(CartRepository cartRepo, CartItemRepository cartItemRepo,
                       ProductRepository productRepo, ComboPackageRepository comboRepo,
                       UserService userService) {
        this.cartRepo = cartRepo;
        this.cartItemRepo = cartItemRepo;
        this.productRepo = productRepo;
        this.comboRepo = comboRepo;
        this.userService = userService;
    }

    @Transactional
    public Cart getOrCreateCart(String email, String sessionId) {
        if (email != null && !email.isBlank()) {
            User user = userService.getByEmail(email);
            return cartRepo.findByUserId(user.getId()).orElseGet(() -> {
                Cart cart = new Cart();
                cart.setUser(user);
                return cartRepo.save(cart);
            });
        }
        return cartRepo.findBySessionId(sessionId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setSessionId(sessionId);
            return cartRepo.save(cart);
        });
    }

    @Transactional
    public void addItem(String email, String sessionId, Long productId, int quantity, String size, String color) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.isActive() || product.getStock() < quantity) {
            throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
        }

        Cart cart = getOrCreateCart(email, sessionId);

        String selSize = (size != null && !size.isBlank()) ? size : "M";
        String selColor = (color != null && !color.isBlank()) ? color : "Black";

        Optional<CartItem> existingItem = cartItemRepo
                .findByCartIdAndProductIdAndSelectedSizeAndSelectedColor(cart.getId(), productId, selSize, selColor);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;
            if (newQuantity > product.getStock()) {
                throw new InsufficientStockException("Cannot add more than available stock (" + product.getStock() + ")");
            }
            item.setQuantity(newQuantity);
            cartItemRepo.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setCombo(false);
            newItem.setQuantity(quantity);
            newItem.setSelectedSize(selSize);
            newItem.setSelectedColor(selColor);
            newItem.setUnitPrice(product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice());
            cart.getItems().add(newItem);
            cartItemRepo.save(newItem);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepo.save(cart);
    }

    @Transactional
    public void addComboItem(String email, String sessionId, Long comboId, int quantity, String comboSelections) {
        addComboItem(email, sessionId, comboId, quantity, comboSelections, null, null);
    }

    @Transactional
    public void addComboItem(String email, String sessionId, Long comboId, int quantity, String comboSelections,
                             BigDecimal customPrice, String includedProductIds) {
        ComboPackage combo = comboRepo.findById(comboId)
                .orElseThrow(() -> new ResourceNotFoundException("Combo package not found with ID: " + comboId));

        if (!combo.isActive()) {
            throw new InsufficientStockException("This combo package is currently unavailable.");
        }

        Cart cart = getOrCreateCart(email, sessionId);

        BigDecimal priceToUse = (customPrice != null && customPrice.compareTo(BigDecimal.ZERO) > 0)
                ? customPrice
                : combo.getEffectivePrice();

        CartItem newItem = new CartItem();
        newItem.setCart(cart);
        newItem.setComboPackage(combo);
        newItem.setCombo(true);
        if (combo.getItems() != null && !combo.getItems().isEmpty() && combo.getItems().get(0).getProduct() != null) {
            newItem.setProduct(combo.getItems().get(0).getProduct());
        }
        newItem.setComboSelections(comboSelections != null ? comboSelections : "Standard Sizes");
        newItem.setIncludedProductIds(includedProductIds);
        newItem.setSelectedSize("Combo");
        newItem.setSelectedColor("Bundle");
        newItem.setQuantity(quantity);
        newItem.setUnitPrice(priceToUse);

        cart.getItems().add(newItem);
        cartItemRepo.save(newItem);

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepo.save(cart);
    }

    @Transactional
    public void updateQuantity(Long itemId, int quantity) {
        CartItem item = cartItemRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (quantity <= 0) {
            cartItemRepo.delete(item);
            return;
        }

        if (item.isCombo() && item.getComboPackage() != null) {
            if (quantity > item.getComboPackage().getAvailableStock()) {
                throw new InsufficientStockException("Cannot add more than available stock for this bundle (" + item.getComboPackage().getAvailableStock() + ")");
            }
        } else if (item.getProduct() != null && quantity > item.getProduct().getStock()) {
            throw new InsufficientStockException("Cannot add more than available stock (" + item.getProduct().getStock() + ")");
        }

        item.setQuantity(quantity);
        cartItemRepo.save(item);
    }

    @Transactional
    public void removeItem(Long itemId) {
        cartItemRepo.deleteById(itemId);
    }

    @Transactional
    public void clearCart(Cart cart) {
        if (cart != null && cart.getItems() != null) {
            cartItemRepo.deleteAll(cart.getItems());
            cart.getItems().clear();
            cartRepo.save(cart);
        }
    }

    @Transactional
    public void mergeGuestCartToUser(String sessionId, String email) {
        if (sessionId == null || email == null) return;

        Optional<Cart> guestCartOpt = cartRepo.findBySessionId(sessionId);
        if (guestCartOpt.isEmpty() || guestCartOpt.get().getItems().isEmpty()) return;

        Cart guestCart = guestCartOpt.get();
        Cart userCart = getOrCreateCart(email, null);

        for (CartItem guestItem : guestCart.getItems()) {
            if (guestItem.isCombo()) {
                addComboItem(email, null, guestItem.getComboPackage().getId(), guestItem.getQuantity(), guestItem.getComboSelections());
            } else {
                addItem(email, null, guestItem.getProduct().getId(), guestItem.getQuantity(),
                        guestItem.getSelectedSize(), guestItem.getSelectedColor());
            }
        }

        cartItemRepo.deleteAll(guestCart.getItems());
        cartRepo.delete(guestCart);
    }
}
