package com.techoworld.eightythree.config;

import com.techoworld.eightythree.processor.CustomerProcessor;
import com.techoworld.eightythree.processor.InvoiceProcessor;
import com.techoworld.eightythree.processor.ProductProcessor;
import com.techoworld.eightythree.provider.CustomerEdmProvider;
import com.techoworld.eightythree.provider.InvoiceEdmProvider;
import com.techoworld.eightythree.provider.ProductEdmProvider;
import com.techoworld.eightythree.servlet.GenericODataServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ODataServletConfig {

    @Bean
    public ServletRegistrationBean<GenericODataServlet> customersOData(CustomerProcessor customerProcessor) {
        GenericODataServlet servlet = new GenericODataServlet(new CustomerEdmProvider(), List.of(customerProcessor));
        ServletRegistrationBean<GenericODataServlet> bean =
                new ServletRegistrationBean<>(servlet, "/odata/customers/*");
        bean.setName("CustomersOData");
        return bean;
    }

    @Bean
    public ServletRegistrationBean<GenericODataServlet> productsOData(ProductProcessor productProcessor) {
        GenericODataServlet servlet = new GenericODataServlet(new ProductEdmProvider(), List.of(productProcessor));
        ServletRegistrationBean<GenericODataServlet> bean =
                new ServletRegistrationBean<>(servlet, "/odata/products/*");
        bean.setName("ProductsOData");
        return bean;
    }

    @Bean
    public ServletRegistrationBean<GenericODataServlet> invoicesOData(InvoiceProcessor invoiceProcessor) {
        GenericODataServlet servlet = new GenericODataServlet(new InvoiceEdmProvider(), List.of(invoiceProcessor));
        ServletRegistrationBean<GenericODataServlet> bean =
                new ServletRegistrationBean<>(servlet, "/odata/invoices/*");
        bean.setName("InvoicesOData");
        return bean;
    }
}
