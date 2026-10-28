package vn.tuan;
import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;import org.springframework.test.context.ActiveProfiles;import org.springframework.test.web.servlet.MockMvc;import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") class SecurityWebTest {
 @Autowired MockMvc mvc;
 @Test void homepageRenders()throws Exception{mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("index"));}
 @Test void ratingSortRenders()throws Exception{mvc.perform(get("/").param("sort","rating")).andExpect(status().isOk());}
 @Test void favoriteSortRenders()throws Exception{mvc.perform(get("/").param("sort","favorite")).andExpect(status().isOk());}
 @Test void managementNeedsLogin()throws Exception{mvc.perform(get("/console/orders")).andExpect(status().is3xxRedirection());}
 @Test void cartPostRequiresCsrf()throws Exception{mvc.perform(post("/cart/remove").param("itemId","1")).andExpect(status().isForbidden());}
 @Test void apiNeedsBearer()throws Exception{mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());}
}
