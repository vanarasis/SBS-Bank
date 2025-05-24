package com.example.SBS.Service;

import com.example.SBS.DTO.AdminDTO;
import com.example.SBS.Entity.Admin;
import com.example.SBS.Repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Inject BCrypt bean

    public Admin createAdmin(AdminDTO adminDTO) {
        String hashedPassword = passwordEncoder.encode(adminDTO.getPassword());

        Admin admin = new Admin(
                adminDTO.getUsername(),
                adminDTO.getEmail(),
                hashedPassword
        );

        return adminRepository.save(admin);
    }

    public Admin getAdminById(Long id) {
        return adminRepository.findById(id).orElse(null);
    }

    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    public void deleteAdmin(Long id) {
        adminRepository.deleteById(id);
    }
}
