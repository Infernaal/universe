package com.fluffy.universe.controllers;

import com.fluffy.universe.controllers.HomeController;
import com.fluffy.universe.services.PostService;
import com.fluffy.universe.utils.SessionUtils;
import com.fluffy.universe.utils.ServerData; // убедитесь, что этот класс существует
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class HomeControllerTest {
    @Test
    public void testHomePageWithValidParams() {
        try (MockedStatic<PostService> postServiceMock = mockStatic(PostService.class);
             MockedStatic<SessionUtils> sessionUtilsMock = mockStatic(SessionUtils.class)) {

            Javalin mockJavalin = mock(Javalin.class);
            HomeController homeController = new HomeController(mockJavalin);

            Context mockContext = mock(Context.class);
            when(mockContext.queryParam("page")).thenReturn("2");
            when(mockContext.queryParam("size")).thenReturn("8");

            // Подготавливаем фиктивные данные для постов и их количества
            int fakePostCount = 20;
            postServiceMock.when(PostService::getPostCount).thenReturn(fakePostCount);
            List<Map<String, Object>> fakePosts = new ArrayList<>();
            fakePosts.add(new HashMap<>());
            postServiceMock.when(() -> PostService.getUserPosts(2, 8)).thenReturn(fakePosts);

            // Подготавливаем фиктивную модель и серверные данные (тип ServerData)
            Map<String, Object> fakeModel = new HashMap<>();
            ServerData fakeServerData = mock(ServerData.class);
            sessionUtilsMock.when(() -> SessionUtils.getCurrentModel(mockContext))
                    .thenReturn(fakeModel);
            sessionUtilsMock.when(() -> SessionUtils.getCurrentServerData(mockContext))
                    .thenReturn(fakeServerData);

            // Act: вызов метода homePage
            homeController.homePage(mockContext);

            // Assert: проверяем, что модель заполнена корректно
            assertEquals(fakePosts, fakeModel.get("posts"));
            assertEquals(fakePostCount, fakeModel.get("paginationRecordCount"));
            assertEquals(8, fakeModel.get("paginationPageSize"));
            assertEquals(2, fakeModel.get("paginationCurrentPage"));
            assertEquals(2, fakeModel.get("paginationSpread"));
            assertEquals("/", fakeModel.get("paginationBaseURL"));
            assertEquals(Arrays.asList(4, 8, 12), fakeModel.get("paginationPageSizeOptions"));

            // Проверяем вызов render и очистку серверных данных
            verify(mockContext).render("/views/pages/home.vm", fakeModel);
            verify(fakeServerData).clear();
        }
    }

    @Test
    public void testHomePageWithInvalidParams() {
        try (MockedStatic<PostService> postServiceMock = mockStatic(PostService.class);
             MockedStatic<SessionUtils> sessionUtilsMock = mockStatic(SessionUtils.class)) {

            Javalin mockJavalin = mock(Javalin.class);
            HomeController homeController = new HomeController(mockJavalin);

            Context mockContext = mock(Context.class);
            when(mockContext.queryParam("page")).thenReturn("invalid");
            when(mockContext.queryParam("size")).thenReturn("invalid");

            int fakePostCount = 15;
            postServiceMock.when(PostService::getPostCount).thenReturn(fakePostCount);
            // Значения по умолчанию: page=1, size=10, но 10 не входит в список, поэтому size становится 4.
            List<Map<String, Object>> fakePosts = new ArrayList<>();
            fakePosts.add(new HashMap<>());
            postServiceMock.when(() -> PostService.getUserPosts(1, 4)).thenReturn(fakePosts);

            Map<String, Object> fakeModel = new HashMap<>();
            ServerData fakeServerData = mock(ServerData.class);
            sessionUtilsMock.when(() -> SessionUtils.getCurrentModel(mockContext))
                    .thenReturn(fakeModel);
            sessionUtilsMock.when(() -> SessionUtils.getCurrentServerData(mockContext))
                    .thenReturn(fakeServerData);

            homeController.homePage(mockContext);

            assertEquals(fakePosts, fakeModel.get("posts"));
            assertEquals(fakePostCount, fakeModel.get("paginationRecordCount"));
            assertEquals(4, fakeModel.get("paginationPageSize"));
            assertEquals(1, fakeModel.get("paginationCurrentPage"));
            assertEquals(2, fakeModel.get("paginationSpread"));
            assertEquals("/", fakeModel.get("paginationBaseURL"));
            assertEquals(Arrays.asList(4, 8, 12), fakeModel.get("paginationPageSizeOptions"));

            verify(mockContext).render("/views/pages/home.vm", fakeModel);
            verify(fakeServerData).clear();
        }
    }

    /**
     * Тест, когда запрошенный номер страницы превышает максимально возможный.
     * Например, при page=10, size=8 и при общем количестве постов, позволяющем максимум 2 страницы, номер страницы корректируется до 2.
     */
    @Test
    public void testHomePagePageNumberExceedsMax() {
        try (MockedStatic<PostService> postServiceMock = mockStatic(PostService.class);
             MockedStatic<SessionUtils> sessionUtilsMock = mockStatic(SessionUtils.class)) {

            Javalin mockJavalin = mock(Javalin.class);
            HomeController homeController = new HomeController(mockJavalin);

            Context mockContext = mock(Context.class);
            when(mockContext.queryParam("page")).thenReturn("10");
            when(mockContext.queryParam("size")).thenReturn("8");

            int fakePostCount = 15; // Максимальная страница = ceil(15/8) = 2
            postServiceMock.when(PostService::getPostCount).thenReturn(fakePostCount);
            List<Map<String, Object>> fakePosts = new ArrayList<>();
            fakePosts.add(new HashMap<>());
            postServiceMock.when(() -> PostService.getUserPosts(2, 8)).thenReturn(fakePosts);

            Map<String, Object> fakeModel = new HashMap<>();
            ServerData fakeServerData = mock(ServerData.class);
            sessionUtilsMock.when(() -> SessionUtils.getCurrentModel(mockContext))
                    .thenReturn(fakeModel);
            sessionUtilsMock.when(() -> SessionUtils.getCurrentServerData(mockContext))
                    .thenReturn(fakeServerData);

            homeController.homePage(mockContext);

            assertEquals(fakePosts, fakeModel.get("posts"));
            assertEquals(fakePostCount, fakeModel.get("paginationRecordCount"));
            assertEquals(8, fakeModel.get("paginationPageSize"));
            assertEquals(2, fakeModel.get("paginationCurrentPage")); // корректировка до максимума
            assertEquals(2, fakeModel.get("paginationSpread"));
            assertEquals("/", fakeModel.get("paginationBaseURL"));
            assertEquals(Arrays.asList(4, 8, 12), fakeModel.get("paginationPageSizeOptions"));

            verify(mockContext).render("/views/pages/home.vm", fakeModel);
            verify(fakeServerData).clear();
        }
    }
}