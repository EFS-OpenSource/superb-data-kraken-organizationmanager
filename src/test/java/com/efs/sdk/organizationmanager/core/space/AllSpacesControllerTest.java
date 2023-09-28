package com.efs.sdk.organizationmanager.core.space;

import com.efs.sdk.organizationmanager.core.OrganizationManagerService;
import com.efs.sdk.organizationmanager.helper.AuthHelper;
import com.efs.sdk.organizationmanager.helper.AuthenticationModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static com.efs.sdk.organizationmanager.core.space.AllSpacesController.ENDPOINT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AllSpacesController.class)
@ActiveProfiles("test")
class AllSpacesControllerTest {
    @MockBean
    private OrganizationManagerService orgaManagerService;
    @MockBean
    private AuthHelper authHelper;
    @Autowired
    private MockMvc mvc;

    @Test
    void givenAuthentication_whenGetAllSpaces_thenOk() throws Exception {
        String orgaName = "orgaName";
        AuthenticationModel authModel = new AuthenticationModel();

        given(orgaManagerService.getSpaceNamesWithOrganizationPrefix(any(), any())).willReturn(Collections.singletonList("org_spc"));

        given(authHelper.getAuthenticationModel(any())).willReturn(authModel);

        mvc.perform(get(ENDPOINT).with(jwt())).andExpect(status().isOk());
    }
}