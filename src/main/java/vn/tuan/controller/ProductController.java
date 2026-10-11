package vn.tuan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.tuan.entity.Product;
import vn.tuan.repository.ProductRepository;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/{id}")
    public String showProductDetail(@PathVariable("id") Integer id, Model model) {
        // Tìm sản phẩm theo id, nếu không có chuyển về trang chủ
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return "redirect:/";
        }

        // Lấy danh sách 4 món tương tự cùng loại
        List<Product> relatedProducts = productRepository.findTop4ByCategoryAndIdNotAndStatus(
                product.getCategory(), product.getId(), "ACTIVE"
        );

        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", relatedProducts);

        return "product-detail";
    }
}