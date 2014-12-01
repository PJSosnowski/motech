package org.motechproject.mds.service.impl;

import org.motechproject.mds.domain.Entity;
import org.motechproject.mds.domain.Field;
import org.motechproject.mds.ex.EntityNotFoundException;
import org.motechproject.mds.ex.ServiceNotFoundException;
import org.motechproject.mds.ex.csv.CsvExportException;
import org.motechproject.mds.ex.csv.CsvImportException;
import org.motechproject.mds.javassist.MotechClassPool;
import org.motechproject.mds.repository.AllEntities;
import org.motechproject.mds.service.CsvImportExportService;
import org.motechproject.mds.service.MotechDataService;
import org.motechproject.mds.util.Constants;
import org.motechproject.mds.util.PropertyUtil;
import org.motechproject.mds.util.ServiceUtil;
import org.motechproject.mds.util.TypeHelper;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the @{link CsvImportExportServiceImpl}.
 * Uses the SuperCSV library for handling CSV files.
 */
@Service
public class CsvImportExportServiceImpl implements CsvImportExportService {

    @Autowired
    private AllEntities allEntities;

    @Autowired
    private BundleContext bundleContext;

    @Override
    @Transactional
    public long exportCsv(long entityId, Writer writer) {
        final Entity entity = getEntity(entityId);
        final MotechDataService dataService = getDataService(entity);

        final String[] headers = fieldsToHeaders(entity.getFields());

        try (CsvMapWriter csvMapWriter = new CsvMapWriter(writer, CsvPreference.STANDARD_PREFERENCE)) {
            csvMapWriter.writeHeader(headers);

            long rowsExported = 0;
            Map<String, String> row = new HashMap<>();

            for (Object instance : dataService.retrieveAll()) {
                buildCsvRow(row, instance, headers);
                csvMapWriter.write(row, headers);
                rowsExported++;
            }

            return rowsExported;
        } catch (IOException e) {
            throw new CsvExportException("IO Error when writing CSV", e);
        }
    }

    @Override
    @Transactional
    public long importCsv(long entityId, Reader reader) {
        final Entity entity = getEntity(entityId);
        final MotechDataService dataService = getDataService(entity);

        final List<Field> fields = entity.getFields();
        final String[] headers = fieldsToHeaders(fields);
        final Class entityClass =dataService.getClassType();

        try (CsvMapReader csvMapReader = new CsvMapReader(reader, CsvPreference.STANDARD_PREFERENCE)) {

            long rowsImported = 0;
            Map<String, String> row;

            while ((row = csvMapReader.read(headers)) != null) {
                Object instance = buildInstanceFromRow(row, fields, entityClass);

                Object id = PropertyUtil.safeGetProperty(instance, Constants.Util.ID_FIELD_NAME);
                if (id == null) {
                    dataService.create(instance);
                } else {
                    dataService.updateFromTransient(instance);
                }

                rowsImported++;
            }

            return rowsImported;
        } catch (IOException e) {
            throw new CsvImportException("IO Error when importing CSV", e);
        }
    }

    private Entity getEntity(long entityId) {
        Entity entity = allEntities.retrieveById(entityId);
        if (entity == null) {
            throw new EntityNotFoundException();
        }
        return entity;
    }

    private MotechDataService getDataService(Entity entity) {
        String interfaceName = MotechClassPool.getInterfaceName(entity.getClassName());
        MotechDataService dataService = ServiceUtil.getServiceForInterfaceName(bundleContext, interfaceName);
        if (dataService == null) {
            throw new ServiceNotFoundException();
        }
        return dataService;
    }

    private String[] fieldsToHeaders(List<Field> fields) {
        List<String> fieldNames = new ArrayList<>();
        for (Field field : fields) {
            fieldNames.add(field.getName());
        }
        return fieldNames.toArray(new String[fieldNames.size()]);
    }

    private void buildCsvRow(Map<String, String> row, Object instance, String[] headers) {
        row.clear();
        for (String fieldName : headers) {
            Object value = PropertyUtil.safeGetProperty(instance, fieldName);
            row.put(fieldName, TypeHelper.format(value));
        }
    }

    private Object buildInstanceFromRow(Map<String, String> row, List<Field> fields, Class entityClass) {
        Object instance;
        try {
            instance = entityClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new CsvImportException("Unable to create instance of " + entityClass.getName(), e);
        }


        for (Field field : fields) {
            String fieldName = field.getName();
            if (row.containsKey(fieldName)) {
                String csvValue = row.get(field.getName());
                Object parsedValue = TypeHelper.parse(csvValue, field.getType().getTypeClass());

                try {
                    PropertyUtil.setProperty(instance, fieldName, parsedValue);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    String msg = String.format("Error when processing field: %s, value in CSV file is %s",
                            field, csvValue);
                    throw new CsvImportException(msg, e);
                }
            }
        }

        return instance;
    }
}