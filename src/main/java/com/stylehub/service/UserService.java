package com.stylehub.service;

import com.stylehub.dto.AddressDto;
import com.stylehub.dto.PasswordChangeDto;
import com.stylehub.dto.RegisterDto;
import com.stylehub.exception.ResourceNotFoundException;
import com.stylehub.model.Address;
import com.stylehub.model.Role;
import com.stylehub.model.User;
import com.stylehub.repo.AddressRepository;
import com.stylehub.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final AddressRepository addressRepo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository userRepo, AddressRepository addressRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.addressRepo = addressRepo;
        this.encoder = encoder;
    }

    public User getByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public User getById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    @Transactional
    public User register(RegisterDto dto) {
        if (userRepo.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("An account with this email address already exists.");
        }
        User user = new User();
        user.setName(dto.getName().trim());
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        return userRepo.save(user);
    }

    @Transactional
    public void updateProfile(String email, String name, String phone, String address) {
        User user = getByEmail(email);
        if (name != null && !name.isBlank()) user.setName(name.trim());
        if (phone != null) user.setPhone(phone.trim());
        if (address != null) user.setAddress(address.trim());
        userRepo.save(user);
    }

    @Transactional
    public void changePassword(String email, PasswordChangeDto dto) {
        User user = getByEmail(email);
        if (!encoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }
        if (dto.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        user.setPassword(encoder.encode(dto.getNewPassword()));
        userRepo.save(user);
    }

    public List<Address> getUserAddresses(Long userId) {
        return addressRepo.findByUserIdOrderByIdDesc(userId);
    }

    @Transactional
    public Address addAddress(String email, String fullName, String phone, String street, String city, String state, String postalCode) {
        User user = getByEmail(email);
        Address address = new Address();
        address.setUser(user);
        address.setFullName(fullName);
        address.setPhone(phone);
        address.setStreetAddress(street);
        address.setCity(city);
        address.setState(state);
        address.setPostalCode(postalCode);

        // If first address, make default
        if (addressRepo.findByUserIdOrderByIdDesc(user.getId()).isEmpty()) {
            address.setDefaultShipping(true);
        }
        return addressRepo.save(address);
    }

    public List<User> getAllCustomers() {
        return userRepo.findByRoleOrderByIdDesc(Role.CUSTOMER);
    }

    public long getCustomerCount() {
        return userRepo.countByRole(Role.CUSTOMER);
    }
}
