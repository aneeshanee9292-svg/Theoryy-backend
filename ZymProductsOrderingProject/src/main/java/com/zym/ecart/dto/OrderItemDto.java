package com.zym.ecart.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDto {
    private Long id;
    private Integer quantity;
    private Double finalPrice;
    private ProductDto product; // ✅ only include product summary
}