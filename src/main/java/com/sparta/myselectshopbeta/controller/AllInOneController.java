package com.sparta.myselectshopbeta.controller;

import com.sparta.myselectshopbeta.dto.ProductMypriceRequestDto;
import com.sparta.myselectshopbeta.dto.ProductRequestDto;
import com.sparta.myselectshopbeta.dto.ProductResponseDto;
import com.sparta.myselectshopbeta.entity.Product;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@RestController // Request를 받는 컨트롤러다.
@RequestMapping("/api") // 모든 URL은 api로 받는다.
public class AllInOneController {

    // (1) 관심 상품 등록하기
    @PostMapping("/products")
    public ProductResponseDto createProduct(@RequestBody ProductRequestDto requestDto) throws SQLException {
        // 요청받은 DTO 로 DB에 저장할 객체 만들기
        Product product = new Product(requestDto);

        // DB 연결 //JPA 없이 Connection과 DriverManager을 사용해서 직접 데이터베이스에 연결하는 것.
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:db", "sa", "");

        // DB Query 작성 // 옛날에 JPA가 없을때는 이런식으로 PreparedStatement 객체로 query 만들고 직접 요청을 보냈음.
        PreparedStatement ps = connection.prepareStatement("select max(id) as id from product"); // 이걸 JPA 쓸땐 JPA가 해주는데, JPA 없으면 직접 id값의 현재 최대값이 뭔지 찾아서 거기에 하나 더 얹어주는 식으로 작업 해야함.
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            // product id 설정 = product 테이블의 마지막 id + 1
            product.setId(rs.getLong("id") + 1);
        } else {
            throw new SQLException("product 테이블의 마지막 id 값을 찾아오지 못했습니다.");
        }

        ps = connection.prepareStatement("insert into product(id, title, image, link, lprice, myprice) values(?, ?, ?, ?, ?, ?)"); // ? 부분에 아래 1~6 정보를 넣어줄거야.
        ps.setLong(1, product.getId());
        ps.setString(2, product.getTitle());
        ps.setString(3, product.getImage());
        ps.setString(4, product.getLink());
        ps.setInt(5, product.getLprice());
        ps.setInt(6, product.getMyprice());

        // DB Query 실행
        ps.executeUpdate();

        // DB 연결 해제
        ps.close();
        connection.close();

        // 응답 보내기
        return new ProductResponseDto(product);
    }

    // (2)관심 상품 조회하기
    @GetMapping("/products")
    public List<ProductResponseDto> getProducts() throws SQLException {
        List<ProductResponseDto> products = new ArrayList<>();

        // DB 연결
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:db", "sa", "");

        // DB Query 작성 및 실행
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("select * from product");

        // DB Query 결과를 상품 객체 리스트로 변환 // 쿼리에서 가져온 RS에 들어있는 값을 새 Product 객체를 만들어서 거기 필드값으로 하나씩 넣어줌.
        while (rs.next()) {
            Product product = new Product();
            product.setId(rs.getLong("id"));
            product.setImage(rs.getString("image"));
            product.setLink(rs.getString("link"));
            product.setLprice(rs.getInt("lprice"));
            product.setMyprice(rs.getInt("myprice"));
            product.setTitle(rs.getString("title"));
            products.add(new ProductResponseDto(product));
        }

        // DB 연결 해제 // 계속 열어두면 리소스 낭비이기 때문에, 닫아주는 행위까지 해야함. (JPA를 사용하면 이런류 반복작업을 안할 수 있어.)
        rs.close();
        connection.close();

        // 응답 보내기
        return products;
    }

    // 관심 상품 최저가 등록하기
    @PutMapping("/products/{id}")
    public Long updateProduct(@PathVariable Long id, @RequestBody ProductMypriceRequestDto requestDto) throws SQLException {
        Product product = new Product();
        // DB 연결
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:db", "sa", "");

        // DB Query 작성
        PreparedStatement ps = connection.prepareStatement("select * from product where id = ?"); // where를 써서 등록을 해야할 프로덕트의 id를 받아.
        ps.setLong(1, id);

        // DB Query 실행
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            product.setId(rs.getLong("id"));
            product.setImage(rs.getString("image"));
            product.setLink(rs.getString("link"));
            product.setLprice(rs.getInt("lprice"));
            product.setMyprice(rs.getInt("myprice"));
            product.setTitle(rs.getString("title"));
        } else {
            throw new NullPointerException("해당 아이디가 존재하지 않습니다.");
        }

        // DB Query 작성
        ps = connection.prepareStatement("update product set myprice = ? where id = ?");
        ps.setInt(1, requestDto.getMyprice());
        ps.setLong(2, product.getId());

        // DB Query 실행
        ps.executeUpdate();

        // DB 연결 해제
        rs.close();
        ps.close();
        connection.close();

        // 응답 보내기 (업데이트된 상품 id)
        return product.getId();
    }



}