package com.fluffy.universe.controllers;

import com.fluffy.universe.exceptions.HttpException;
import com.fluffy.universe.models.Post;
import com.fluffy.universe.models.User;
import com.fluffy.universe.services.CommentService;
import com.fluffy.universe.services.PostService;
import com.fluffy.universe.utils.AlertType;
import com.fluffy.universe.utils.SecurityUtils;
import com.fluffy.universe.utils.ServerData;
import com.fluffy.universe.utils.SessionUtils;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostControllerTest {

    /**
     * Проверяем, что метод indexPage перенаправляет на главную страницу.
     */
    @Test
    public void testIndexPage_redirectsToRoot() {
        Context ctx = mock(Context.class);
        PostController controller = new PostController(mock(Javalin.class));
        controller.indexPage(ctx);
        verify(ctx).redirect("/");
    }

    /**
     * Проверяем, что метод createPage вызывает рендеринг страницы создания поста.
     */
    @Test
    public void testCreatePage_rendersCreateView() {
        Context ctx = mock(Context.class);
        // Создаём spy для перехвата вызова метода render, определённого в базовом классе Controller
        PostController controller = spy(new PostController(mock(Javalin.class)));
        doNothing().when(controller).render(any(Context.class), anyString());
        controller.createPage(ctx);
        verify(controller).render(ctx, "/views/pages/models/post/create.vm");
    }

    /**
     * Проверяем, что метод editPage вызывает рендеринг страницы редактирования поста.
     */
    @Test
    public void testEditPage_rendersEditView() {
        Context ctx = mock(Context.class);
        PostController controller = spy(new PostController(mock(Javalin.class)));
        doNothing().when(controller).render(any(Context.class), anyString());
        controller.editPage(ctx);
        verify(controller).render(ctx, "/views/pages/models/post/edit.vm");
    }

    /**
     * Проверяем сценарий, когда пост не найден – метод show должен выбросить HttpException с кодом NOT_FOUND.
     */
    @Test
    public void testShow_postNotFound_throwsHttpException() {
        Context ctx = mock(Context.class);
        when(ctx.pathParam("post")).thenReturn("1");

        // Мокаем статический вызов PostService.getUserPost
        try (MockedStatic<PostService> postServiceMock = Mockito.mockStatic(PostService.class)) {
            postServiceMock.when(() -> PostService.getUserPost(1)).thenReturn(null);
            HttpException thrown = assertThrows(HttpException.class, () -> {
                new PostController(mock(Javalin.class)).show(ctx);
            });
            assertEquals(HttpCode.NOT_FOUND.getStatus(), thrown.getHttpCode().getStatus());
        }
    }

    /**
     * Проверяем корректное выполнение метода show, когда пост найден.
     */
    @Test
    public void testShow_postFound_rendersShowView() {
        Context ctx = mock(Context.class);
        when(ctx.pathParam("post")).thenReturn("1");

        // Подготовка фиктивных данных
        Map<String, Object> userPost = new HashMap<>();
        userPost.put("post.id", 1);
        userPost.put("title", "Test Title");

        List<Map<String, Object>> comments = new ArrayList<>();
        Map<String, Object> comment = new HashMap<>();
        comment.put("comment", "Nice post!");
        comments.add(comment);

        // Мокаем статические методы
        try (MockedStatic<PostService> postServiceMock = Mockito.mockStatic(PostService.class);
             MockedStatic<CommentService> commentServiceMock = Mockito.mockStatic(CommentService.class);
             MockedStatic<SessionUtils> sessionUtilsMock = Mockito.mockStatic(SessionUtils.class)) {

            postServiceMock.when(() -> PostService.getUserPost(1)).thenReturn(userPost);
            commentServiceMock.when(() -> CommentService.getUserCommentsByPostId(1)).thenReturn(comments);

            Map<String, Object> model = new HashMap<>();
            sessionUtilsMock.when(() -> SessionUtils.getCurrentModel(ctx)).thenReturn(model);

            PostController controller = spy(new PostController(mock(Javalin.class)));
            doNothing().when(controller).render(any(Context.class), anyString());

            controller.show(ctx);
            // Проверяем, что в модель добавлены данные поста и комментариев
            assertEquals(userPost, model.get("post"));
            assertEquals(comments, model.get("comments"));
            verify(controller).render(ctx, "/views/pages/models/post/show.vm");
        }
    }

    /**
     * Вспомогательный класс для эмуляции пользователя.
     */
    public static class DummyUser extends User {
        public DummyUser(int id) {
            super();
            this.setId(id);
        }
    }

    /**
     * Проверяем выполнение метода store: пост должен быть сохранён, серверным данным установлено уведомление и выполнено перенаправление.
     */
    @Test
    public void testStore_savesPostAndRedirects() {
        Context ctx = mock(Context.class);
        when(ctx.formParam("title")).thenReturn("Test Title");
        when(ctx.formParam("description")).thenReturn("Test Description");

        // Используем корректный объект User
        User dummyUser = new User();
        dummyUser.setId(42);

        ServerData serverData = new ServerData();

        try (MockedStatic<SessionUtils> sessionUtilsMock = Mockito.mockStatic(SessionUtils.class);
             MockedStatic<SecurityUtils> securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
             MockedStatic<PostService> postServiceMock = Mockito.mockStatic(PostService.class)) {

            sessionUtilsMock.when(() -> SessionUtils.getCurrentServerData(ctx)).thenReturn(serverData);
            sessionUtilsMock.when(() -> SessionUtils.getCurrentUser(ctx)).thenReturn(dummyUser);
            securityUtilsMock.when(() -> SecurityUtils.escape("Test Title")).thenReturn("Test Title");
            securityUtilsMock.when(() -> SecurityUtils.escape("Test Description")).thenReturn("Test Description");

            PostController controller = new PostController(mock(Javalin.class));
            controller.store(ctx);

            postServiceMock.verify(() -> PostService.savePost(argThat((Post p) ->
                    p.getUserId() == 42 &&
                            "Test Title".equals(p.getTitle()) &&
                            "Test Description".equals(p.getDescription()) &&
                            p.getPublicationDateTime() != null
            )), times(1));

            verify(ctx).redirect("/");
        }
    }
}
