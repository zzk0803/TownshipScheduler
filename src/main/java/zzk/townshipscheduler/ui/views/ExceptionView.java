package zzk.townshipscheduler.ui.views;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.io.PrintWriter;
import java.io.StringWriter;

@Route(layout = MainLayout.class)
@AnonymousAllowed
public class ExceptionView
        extends VerticalLayout
        implements HasErrorParameter<Exception> {

    private final VerticalLayout errorShowLayout;

    public ExceptionView() {
        add(new Text("Something Broken"));
        errorShowLayout = new VerticalLayout();
        add(errorShowLayout);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent beforeEnterEvent, ErrorParameter<Exception> errorParameter) {
        Exception exception = errorParameter.getException();
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter, true);
        exception.printStackTrace(printWriter);
        exception.printStackTrace();
        errorShowLayout.add(new Paragraph(stringWriter.toString()));
        return HttpStatusCode.INTERNAL_SERVER_ERROR.getCode();
    }

}
