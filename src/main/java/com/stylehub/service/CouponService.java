package com.stylehub.service;

import com.stylehub.model.Coupon;
import com.stylehub.model.DiscountType;
import com.stylehub.repo.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class CouponService {

    private final CouponRepository couponRepo;

    public CouponService(CouponRepository couponRepo) {
        this.couponRepo = couponRepo;
    }

    public List<Coupon> getAll() {
        return couponRepo.findAll();
    }

    public Optional<Coupon> validateCoupon(String code, BigDecimal orderSubtotal) {
        if (code == null || code.isBlank()) return Optional.empty();

        Optional<Coupon> couponOpt = couponRepo.findByCodeIgnoreCaseAndActiveTrue(code.trim());
        if (couponOpt.isEmpty()) return Optional.empty();

        Coupon coupon = couponOpt.get();
        if (orderSubtotal != null && orderSubtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            return Optional.empty();
        }
        return Optional.of(coupon);
    }

    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        if (coupon == null || subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = subtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = coupon.getDiscountValue();
        }

        if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            discount = coupon.getMaxDiscountAmount();
        }

        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        return discount;
    }

    @Transactional
    public void recordUsage(String code) {
        couponRepo.findByCodeIgnoreCase(code).ifPresent(c -> {
            c.setUsedCount(c.getUsedCount() + 1);
            couponRepo.save(c);
        });
    }

    @Transactional
    public Coupon save(Coupon coupon) {
        if (coupon.getCode() != null) {
            coupon.setCode(coupon.getCode().trim().toUpperCase());
        }
        return couponRepo.save(coupon);
    }

    @Transactional
    public void delete(Long id) {
        couponRepo.deleteById(id);
    }
}
