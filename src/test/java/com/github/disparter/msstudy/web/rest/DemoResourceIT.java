package com.github.disparter.msstudy.web.rest;

import com.github.disparter.msstudy.MsstudyApp;
import com.github.disparter.msstudy.domain.Demo;
import com.github.disparter.msstudy.repository.DemoRepository;
import com.github.disparter.msstudy.service.DemoService;
import com.github.disparter.msstudy.web.rest.errors.ExceptionTranslator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.util.List;

import static com.github.disparter.msstudy.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@Link DemoResource} REST controller.
 */
@SpringBootTest(classes = MsstudyApp.class)
public class DemoResourceIT {

    private static final String DEFAULT_DEMOFIELD = "AAAAAAAAAA";
    private static final String UPDATED_DEMOFIELD = "BBBBBBBBBB";

    @Autowired
    private DemoRepository demoRepository;

    @Autowired
    private DemoService demoService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    private MockMvc restDemoMockMvc;

    private Demo demo;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final DemoResource demoResource = new DemoResource(demoService);
        this.restDemoMockMvc = MockMvcBuilders.standaloneSetup(demoResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Demo createEntity(EntityManager em) {
        Demo demo = new Demo()
            .demofield(DEFAULT_DEMOFIELD);
        return demo;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Demo createUpdatedEntity(EntityManager em) {
        Demo demo = new Demo()
            .demofield(UPDATED_DEMOFIELD);
        return demo;
    }

    @BeforeEach
    public void initTest() {
        demo = createEntity(em);
    }

    @Test
    @Transactional
    public void createDemo() throws Exception {
        int databaseSizeBeforeCreate = demoRepository.findAll().size();

        // Create the Demo
        restDemoMockMvc.perform(post("/api/demos")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(demo)))
            .andExpect(status().isCreated());

        // Validate the Demo in the database
        List<Demo> demoList = demoRepository.findAll();
        assertThat(demoList).hasSize(databaseSizeBeforeCreate + 1);
        Demo testDemo = demoList.get(demoList.size() - 1);
        assertThat(testDemo.getDemofield()).isEqualTo(DEFAULT_DEMOFIELD);
    }

    @Test
    @Transactional
    public void createDemoWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = demoRepository.findAll().size();

        // Create the Demo with an existing ID
        demo.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restDemoMockMvc.perform(post("/api/demos")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(demo)))
            .andExpect(status().isBadRequest());

        // Validate the Demo in the database
        List<Demo> demoList = demoRepository.findAll();
        assertThat(demoList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void getAllDemos() throws Exception {
        // Initialize the database
        demoRepository.saveAndFlush(demo);

        // Get all the demoList
        restDemoMockMvc.perform(get("/api/demos?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(demo.getId().intValue())))
            .andExpect(jsonPath("$.[*].demofield").value(hasItem(DEFAULT_DEMOFIELD.toString())));
    }
    
    @Test
    @Transactional
    public void getDemo() throws Exception {
        // Initialize the database
        demoRepository.saveAndFlush(demo);

        // Get the demo
        restDemoMockMvc.perform(get("/api/demos/{id}", demo.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(demo.getId().intValue()))
            .andExpect(jsonPath("$.demofield").value(DEFAULT_DEMOFIELD.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingDemo() throws Exception {
        // Get the demo
        restDemoMockMvc.perform(get("/api/demos/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateDemo() throws Exception {
        // Initialize the database
        demoService.save(demo);

        int databaseSizeBeforeUpdate = demoRepository.findAll().size();

        // Update the demo
        Demo updatedDemo = demoRepository.findById(demo.getId()).get();
        // Disconnect from session so that the updates on updatedDemo are not directly saved in db
        em.detach(updatedDemo);
        updatedDemo
            .demofield(UPDATED_DEMOFIELD);

        restDemoMockMvc.perform(put("/api/demos")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedDemo)))
            .andExpect(status().isOk());

        // Validate the Demo in the database
        List<Demo> demoList = demoRepository.findAll();
        assertThat(demoList).hasSize(databaseSizeBeforeUpdate);
        Demo testDemo = demoList.get(demoList.size() - 1);
        assertThat(testDemo.getDemofield()).isEqualTo(UPDATED_DEMOFIELD);
    }

    @Test
    @Transactional
    public void updateNonExistingDemo() throws Exception {
        int databaseSizeBeforeUpdate = demoRepository.findAll().size();

        // Create the Demo

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restDemoMockMvc.perform(put("/api/demos")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(demo)))
            .andExpect(status().isBadRequest());

        // Validate the Demo in the database
        List<Demo> demoList = demoRepository.findAll();
        assertThat(demoList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteDemo() throws Exception {
        // Initialize the database
        demoService.save(demo);

        int databaseSizeBeforeDelete = demoRepository.findAll().size();

        // Delete the demo
        restDemoMockMvc.perform(delete("/api/demos/{id}", demo.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<Demo> demoList = demoRepository.findAll();
        assertThat(demoList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Demo.class);
        Demo demo1 = new Demo();
        demo1.setId(1L);
        Demo demo2 = new Demo();
        demo2.setId(demo1.getId());
        assertThat(demo1).isEqualTo(demo2);
        demo2.setId(2L);
        assertThat(demo1).isNotEqualTo(demo2);
        demo1.setId(null);
        assertThat(demo1).isNotEqualTo(demo2);
    }
}
