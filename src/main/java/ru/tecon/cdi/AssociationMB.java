package ru.tecon.cdi;

import org.primefaces.event.SelectEvent;
import ru.tecon.model.association.AssociationCounterModel;
import ru.tecon.model.association.AssociationModel;
import ru.tecon.model.association.AssociationNameModel;
import ru.tecon.sessionBean.ParserLocal;

import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Контроллер для обработки запросов на всплывающие окно удаления ассоциаций
 * @author Maksim Shchelkonogov
 */
@Named("association")
@ViewScoped
public class AssociationMB implements Serializable {

    private String objectName;
    private List<AssociationModel> associationNames = new ArrayList<>();
    private List<AssociationModel> associationCounters = new ArrayList<>();

    @EJB
    private ParserLocal ejbParser;

    public List<String> completeText(String query) {
        return ejbParser.getObjectNames(query);
    }

    /**
     * Метод обрабатывает выбор элемента в элементе autoSelect
     * @param event событие
     */
    public void onItemSelect(SelectEvent<String> event) {
        associationNames = ejbParser.getAssociationNames(event.getObject());
        associationCounters = ejbParser.getAssociationCounters(event.getObject());
    }

    /**
     * Метод удаляет выбранную ассоциацию из базы
     * @param removeAssociate ассоциация
     */
    public void removeAssociate(AssociationModel removeAssociate) {
        System.out.println(removeAssociate);
        if (removeAssociate instanceof AssociationNameModel) {
            associationNames.remove(removeAssociate);
            ejbParser.removeAssociationName(removeAssociate.getRowID());
        }
        if (removeAssociate instanceof AssociationCounterModel) {
            associationCounters.remove(removeAssociate);
            ejbParser.removeAssociationCounter(removeAssociate.getRowID());
        }
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public List<AssociationModel> getAssociationNames() {
        return associationNames;
    }

    public List<AssociationModel> getAssociationCounters() {
        return associationCounters;
    }
}
