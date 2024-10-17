package zzk.project.vaadinproject.ui;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.server.HttpStatusCode;

import java.io.PrintWriter;
import java.io.StringWriter;

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
        exception.printStackTrace();
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter, true);
        exception.printStackTrace(printWriter);
        errorShowLayout.add(new Paragraph(stringWriter.toString()));
        return HttpStatusCode.INTERNAL_SERVER_ERROR.getCode();
    }

}
