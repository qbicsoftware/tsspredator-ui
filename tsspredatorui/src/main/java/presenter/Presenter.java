package presenter;

import com.vaadin.data.*;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.server.Setter;
import com.vaadin.ui.Notification;
import model.beans.AlignmentFileBean;
import model.beans.AnnotationFileBean;
import model.beans.FastaFileBean;
import model.beans.ProjectBean;
import model.config.ConfigFile;
import model.config.Genome;
import model.config.Replicate;
import view.AccordionLayoutMain;
import view.firstImplementation.ConditionDataPanel;
import view.firstImplementation.DataPanel;
import view.firstImplementation.GenomeDataPanel;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Objects;

/**
 * @author jmueller
 */
public class Presenter {
    private ConfigFile configFile;
    private AccordionLayoutMain view;
    private Binder<ConfigFile> configFileBinder;

    public enum Preset {
        VERY_SPECIFIC, MORE_SPECIFIC, DEFAULT, MORE_SENSITIVE, VERY_SENSITIVE

    }

    private Preset preset = Preset.DEFAULT;

    public Presenter() {
        configFile = new ConfigFile();
        configFile.setGenomeList(new ArrayList<>());
        //We start with one Dataset and one replicate
        addDatasets(1);
        addReplicates(1);
        setInitialConfigParameters();
        configFileBinder = new Binder<>();
        configFileBinder.setBean(configFile);
    }

    /**
     * Initializes the fields of the view
     * TODO: Move stuff from the view here so the view contains less logic
     */
    public void initFields() {
        view.getParametersPanel().getPresetOrCustom().setSelectedItem("Preset");
        view.getParametersPanel().getPresetSelection().setSelectedItem("Default");

    }

    public void initBindings() {
        configFileBinder.forField(view.getGeneralConfigPanel().getProjectGrid().asSingleSelect())
                .withValidator(Objects::nonNull, "Please select a project from the list")
                .bind(new ValueProvider<ConfigFile, ProjectBean>() {
                          @Override
                          public ProjectBean apply(ConfigFile configFile) {
                              return new ProjectBean(); //TODO: Return something useful here
                          }
                      },
                        new Setter<ConfigFile, ProjectBean>() {
                            @Override
                            public void accept(ConfigFile configFile, ProjectBean projectBean) {
                                configFile.setProjectName(projectBean.getName());
                            }
                        });

        configFileBinder.forField(view.getGenomeDataPanel().getNumberOfDatasetsBox())
                .asRequired("Please set a number")
                .withValidator(new IntegerRangeValidator("Please set at least to 1", 1, Integer.MAX_VALUE))
                .bind(
                        (ValueProvider<ConfigFile, Integer>) configFile1 -> configFile1.getNumberOfDatasets(),
                        (Setter<ConfigFile, Integer>) (configFile, number) -> {
                            int oldDatasetCount = configFile.getNumberOfDatasets();
                            //Add or remove datasets in the ConfigFile so they match the given number
                            int delta = number - oldDatasetCount;
                            configFile.setNumberOfDatasets(number);
                            if (delta > 0) {
                                addDatasets(delta);
                            } else {
                                removeDatasets(-delta);
                            }
                            //Update view
                            view.getGenomeDataPanel().updateAccordion(oldDatasetCount, getNumberOfReplicates());

                        });
        configFileBinder.forField(view.getConditionDataPanel().getNumberOfDatasetsBox())
                .asRequired("Please set a number")
                .withValidator(new IntegerRangeValidator("Please set at least to 1", 1, Integer.MAX_VALUE))
                .bind(
                        (ValueProvider<ConfigFile, Integer>) configFile1 -> configFile1.getNumberOfDatasets(),
                        (Setter<ConfigFile, Integer>) (configFile, number) -> {
                            int oldDatasetCount = configFile.getNumberOfDatasets();
                            //Add or remove datasets in the ConfigFile so they match the given number
                            int delta = number - oldDatasetCount;
                            configFile.setNumberOfDatasets(number);
                            if (delta > 0)
                                addDatasets(delta);
                            else
                                removeDatasets(-delta);
                            //Update view
                            view.getConditionDataPanel().updateAccordion(oldDatasetCount, getNumberOfReplicates());
                        });

        configFileBinder.forField(view.getGenomeDataPanel().getNumberOfReplicatesBox())
                .asRequired("Please set a number")
                .withValidator(new IntegerRangeValidator("Please set at least to 1", 1, Integer.MAX_VALUE))
                .bind((ValueProvider<ConfigFile, Integer>) configFile1 -> configFile1.getNumberOfReplicates(),
                        (Setter<ConfigFile, Integer>) (configFile, number) -> {
                            int oldReplicateCount = configFile.getNumberOfReplicates();
                            int delta = number - oldReplicateCount;
                            configFile.setNumberOfReplicates(number);
                            //Add or remove replicates so they match the given number
                            if (delta > 0)
                                addReplicates(delta);
                            else
                                removeReplicates(-delta);
                            //Update view
                            view.getGenomeDataPanel().updateAccordion(getNumberOfDatasets(), oldReplicateCount);
                        });

        configFileBinder.forField(view.getConditionDataPanel().getNumberOfReplicatesBox())
                .asRequired("Please set a number")
                .withValidator(new IntegerRangeValidator("Please set at least to 1", 1, Integer.MAX_VALUE))
                .bind(configFile1 -> configFile1.getNumberOfReplicates(),
                        (Setter<ConfigFile, Integer>) (configFile, number) -> {
                            int oldReplicateCount = configFile.getNumberOfReplicates();
                            int delta = number - oldReplicateCount;
                            configFile.setNumberOfReplicates(number);
                            //Add or remove replicates so they match the given number
                            if (delta > 0)
                                addReplicates(delta);
                            else
                                removeReplicates(-delta);
                            view.getConditionDataPanel().updateAccordion(getNumberOfDatasets(), oldReplicateCount);
                        });

        configFileBinder.forField(view.getGeneralConfigPanel().getProjectTypeButtonGroup())
                .bind(new ValueProvider<ConfigFile, String>() {
                          @Override
                          public String apply(ConfigFile configFile) {
                              //TODO: Replace hard-coded strings by global variables (and also replace in GeneralConfigPanel!)
                              return configFile.isModeConditions() ? "Compare Conditions" : "Compare Strain/Species";
                          }
                      },
                        new Setter<ConfigFile, String>() {
                            @Override
                            public void accept(ConfigFile configFile, String s) {
                                boolean isModeConditions = s.equals("Compare Conditions");
                                view.updateDataPanelMode(isModeConditions);
                                configFile.setModeConditions(isModeConditions);
                                view.getGeneralConfigPanel().getAlignmentFileGrid().setVisible(!isModeConditions);
                                if (isModeConditions) {
                                    view.getGenomeDataPanel().getDatasetAccordion().removeAllComponents();
                                    view.getConditionDataPanel().initAccordion();
                                    view.getConditionDataPanel().getNumberOfDatasetsBox().setValue(1);
                                    view.getConditionDataPanel().getNumberOfReplicatesBox().setValue(1);
                                } else {
                                    view.getConditionDataPanel().getDatasetAccordion().removeAllComponents();
                                    view.getGenomeDataPanel().initAccordion();
                                    view.getGenomeDataPanel().getNumberOfDatasetsBox().setValue(1);
                                    view.getConditionDataPanel().getNumberOfReplicatesBox().setValue(1);

                                }

                            }
                        }
                );
        configFileBinder.forField(view.getGeneralConfigPanel().getAlignmentFileGrid().asSingleSelect())
                .withValidator(Objects::nonNull, "Please select an alignment file from the list")
                .bind(new ValueProvider<ConfigFile, AlignmentFileBean>() {
                          @Override
                          public AlignmentFileBean apply(ConfigFile configFile) {
                              return new AlignmentFileBean(); //TODO: Return something useful here
                          }
                      },
                        new Setter<ConfigFile, AlignmentFileBean>() {
                            @Override
                            public void accept(ConfigFile configFile, AlignmentFileBean alignmentFileBean) {
                                configFile.setAlignmentFile(alignmentFileBean.getName());
                            }
                        });

        configFileBinder.forField(view.getParametersPanel().getWriteGraphs())
                .bind(ConfigFile::isWriteGraphs,
                        ConfigFile::setWriteGraphs);
        configFileBinder.forField(view.getParametersPanel().getStepHeight()).bind(
                ConfigFile::getStepHeight,
                ConfigFile::setStepHeight);
        configFileBinder.forField(view.getParametersPanel().getStepHeightReduction()).bind(
                ConfigFile::getStepHeightReduction,
                ConfigFile::setStepHeightReduction);
        configFileBinder.forField(view.getParametersPanel().getStepFactor()).bind(
                ConfigFile::getStepFactor,
                ConfigFile::setStepFactor);
        configFileBinder.forField(view.getParametersPanel().getStepFactorReduction()).bind(
                ConfigFile::getStepFactorReduction,
                ConfigFile::setStepFactorReduction);
        configFileBinder.forField(view.getParametersPanel().getEnrichmentFactor()).bind(
                ConfigFile::getEnrichmentFactor,
                ConfigFile::setEnrichmentFactor);
        configFileBinder.forField(view.getParametersPanel().getProcessingSiteFactor()).bind(
                ConfigFile::getProcessingSiteFactor,
                ConfigFile::setProcessingSiteFactor);
        configFileBinder.forField(view.getParametersPanel().getStepLength())
                .withConverter(Double::intValue, Integer::doubleValue)
                .bind(ConfigFile::getStepLength, ConfigFile::setStepLength);
        configFileBinder.forField(view.getParametersPanel().getBaseHeight()).bind(
                ConfigFile::getBaseHeight,
                ConfigFile::setBaseHeight);
        configFileBinder.forField(view.getParametersPanel().getNormalizationPercentile()).bind(
                ConfigFile::getNormalizationPercentile,
                ConfigFile::setNormalizationPercentile);
        configFileBinder.forField(view.getParametersPanel().getEnrichedNormalizationPercentile()).bind(
                ConfigFile::getEnrichmentNormalizationPercentile,
                ConfigFile::setEnrichmentNormalizationPercentile);
        configFileBinder.forField(view.getParametersPanel().getClusterMethod()).bind(
                ConfigFile::getClusterMethod,
                ConfigFile::setClusterMethod);
        configFileBinder.forField(view.getParametersPanel().getClusteringDistance())
                .withConverter(Double::intValue, Integer::doubleValue)
                .bind(ConfigFile::getTssClusteringDistance, ConfigFile::setTssClusteringDistance);
        configFileBinder.forField(view.getParametersPanel().getCrossDatasetShift())
                .withConverter(Double::intValue, Integer::doubleValue)
                .bind(ConfigFile::getAllowedCrossDatasetShift, ConfigFile::setAllowedCrossDatasetShift);
        configFileBinder.forField(view.getParametersPanel().getCrossReplicateShift())
                .withConverter(Double::intValue, Integer::doubleValue)
                .bind(ConfigFile::getAllowedCrossReplicateShift, ConfigFile::setAllowedCrossReplicateShift);
        configFileBinder.forField(view.getParametersPanel().getMatchingReplicates()).bind(
                ConfigFile::getMatchingReplicates,
                ConfigFile::setMatchingReplicates);
        configFileBinder.forField(view.getParametersPanel().getUtrLength())
                .withConverter(Double::intValue, Integer::doubleValue)
                .bind(ConfigFile::getUtrLength, ConfigFile::setUtrLength);
        configFileBinder.forField(view.getParametersPanel().getAntisenseUtrLength())
                .withConverter(Double::intValue, Integer::doubleValue)
                .bind(ConfigFile::getAntisenseUtrLength, ConfigFile::setAntisenseUtrLength);

        //Bindings for conditionDataPanel exclusively
        configFileBinder.forField(view.getConditionDataPanel().getFastaGrid().asSingleSelect())
                .withValidator(Objects::nonNull, "Please select a Fasta file (*.fasta)")
                .bind(new ValueProvider<ConfigFile, FastaFileBean>() {
                          @Override
                          public FastaFileBean apply(ConfigFile configFile) {
                              return new FastaFileBean();
                          }
                      },
                        new Setter<ConfigFile, FastaFileBean>() {
                            @Override
                            public void accept(ConfigFile configFile, FastaFileBean fastaFileBean) {
                                configFile.setConditionFasta(fastaFileBean.getName());
                            }
                        });
        configFileBinder.forField(view.getConditionDataPanel().getGffGrid().asSingleSelect())
                .withValidator(Objects::nonNull, "Please select an annotation file (*.gff)")
                .bind(new ValueProvider<ConfigFile, AnnotationFileBean>() {
                    @Override
                    public AnnotationFileBean apply(ConfigFile configFile) {
                        return new AnnotationFileBean();
                    }
                }, new Setter<ConfigFile, AnnotationFileBean>() {
                    @Override
                    public void accept(ConfigFile configFile, AnnotationFileBean annotationFileBean) {
                        configFile.setConditionGFF(annotationFileBean.getName());
                    }
                });
    }

    public void setInitialConfigParameters() {
        configFile.setModeConditions(false);
        configFile.setNumberOfDatasets(1);
        configFile.setNumberOfReplicates(1);
        configFile.setNormalizationPercentile(0.9);
        configFile.setEnrichmentNormalizationPercentile(0.5);
        configFile.setClusterMethod("HIGHEST");
        configFile.setTssClusteringDistance(3);
        configFile.setAllowedCrossDatasetShift(1);
        configFile.setAllowedCrossReplicateShift(1);
        configFile.setMatchingReplicates(1);
        configFile.setUtrLength(300);
        configFile.setAntisenseUtrLength(100);
    }

    public void initDatasetBindings(int index) {
        if (configFile.isModeConditions()) {
            ConditionDataPanel.ConditionTab conditionTab = (ConditionDataPanel.ConditionTab) view.getConditionDataPanel().getDatasetTab(index);
            configFileBinder.forField(conditionTab.getNameField()).asRequired("Please enter the name of the condition")
                    .bind(new ValueProvider<ConfigFile, String>() {
                              @Override
                              public String apply(ConfigFile configFile) {
                                  return configFile.getGenomeList().get(index).getName();
                              }
                          },
                            new Setter<ConfigFile, String>() {
                                @Override
                                public void accept(ConfigFile configFile, String name) {
                                    configFile.getGenomeList().get(index).setName(name);
                                }
                            });
        } else {
            GenomeDataPanel.GenomeTab genomeTab = (GenomeDataPanel.GenomeTab) view.getGenomeDataPanel().getDatasetTab(index);
            configFileBinder.forField(genomeTab.getNameField()).asRequired("Please enter the name of the genome")
                    .bind(new ValueProvider<ConfigFile, String>() {
                              @Override
                              public String apply(ConfigFile configFile) {
                                  return configFile.getGenomeList().get(index).getName();
                              }
                          },
                            new Setter<ConfigFile, String>() {
                                @Override
                                public void accept(ConfigFile configFile, String name) {
                                    configFile.getGenomeList().get(index).setName(name);
                                }
                            });

            configFileBinder.forField(genomeTab.getFastaGrid().asSingleSelect())
                    .withValidator(Objects::nonNull, "Please select the fasta file of this genome (*.fasta)")
                    .bind(new ValueProvider<ConfigFile, FastaFileBean>() {
                        @Override
                        public FastaFileBean apply(ConfigFile configFile) {
                            return new FastaFileBean();
                        }
                    }, new Setter<ConfigFile, FastaFileBean>() {
                        @Override
                        public void accept(ConfigFile configFile, FastaFileBean fastaFileBean) {
                            configFile.getGenomeList().get(index).setFasta(fastaFileBean.getName());
                        }
                    });

            configFileBinder.forField(genomeTab.getIdField()).asRequired("Please enter the alignment id of this genome")
                    .bind(new ValueProvider<ConfigFile, String>() {
                              @Override
                              public String apply(ConfigFile configFile) {
                                  return configFile.getGenomeList().get(index).getAlignmentID();
                              }
                          },
                            new Setter<ConfigFile, String>() {
                                @Override
                                public void accept(ConfigFile configFile, String id) {
                                    configFile.getGenomeList().get(index).setAlignmentID(id);
                                }
                            });

            configFileBinder.forField(genomeTab.getGffGrid().asSingleSelect())
                    .withValidator(Objects::nonNull, "Please select the annotation file of this genome (*.gff)")
                    .bind(new ValueProvider<ConfigFile, AnnotationFileBean>() {
                        @Override
                        public AnnotationFileBean apply(ConfigFile configFile) {
                            return new AnnotationFileBean();
                        }
                    }, new Setter<ConfigFile, AnnotationFileBean>() {
                        @Override
                        public void accept(ConfigFile configFile, AnnotationFileBean annotationFileBean) {
                            configFile.getGenomeList().get(index).setGff(annotationFileBean.getName());
                        }
                    });
        }


    }


    public void initReplicateBindings(int datasetIndex, int replicateIndex) {
        DataPanel.ReplicateTab replicateTab;
        if (configFile.isModeConditions()) {
            replicateTab = view.getConditionDataPanel().getDatasetTab(datasetIndex).getReplicateTab(replicateIndex);
        } else {
            replicateTab = view.getGenomeDataPanel().getDatasetTab(datasetIndex).getReplicateTab(replicateIndex);
        }
        configFileBinder.forField(replicateTab.getEnrichedCoding()).asRequired("Please select a graph file (*.gr)")
                .bind(new ValueProvider<ConfigFile, String>() {
                          @Override
                          public String apply(ConfigFile configFile) {
                              return configFile.getGenomeList().get(datasetIndex).getReplicateList().get(replicateIndex).getEnrichedCodingStrand();
                          }
                      },
                        new Setter<ConfigFile, String>() {
                            @Override
                            public void accept(ConfigFile configFile, String strand) {
                                configFile.getGenomeList().get(datasetIndex).getReplicateList().get(replicateIndex).setEnrichedCodingStrand(strand);
                            }
                        });
        configFileBinder.forField(replicateTab.getEnrichedTemplate()).asRequired("Please select a graph file (*.gr)")
                .bind(new ValueProvider<ConfigFile, String>() {
                          @Override
                          public String apply(ConfigFile configFile) {
                              return configFile.getGenomeList().get(datasetIndex).getReplicateList().get(replicateIndex).getEnrichedTemplateStrand();
                          }
                      },
                        new Setter<ConfigFile, String>() {
                            @Override
                            public void accept(ConfigFile configFile, String strand) {
                                configFile.getGenomeList().get(datasetIndex).getReplicateList().get(replicateIndex).setEnrichedTemplateStrand(strand);
                            }
                        });
        configFileBinder.forField(replicateTab.getNormalCoding()).asRequired("Please select a graph file (*.gr)")
                .bind(new ValueProvider<ConfigFile, String>() {
                          @Override
                          public String apply(ConfigFile configFile) {
                              return configFile.getGenomeList().get(datasetIndex).getReplicateList().get(replicateIndex).getNormalCodingStrand();
                          }
                      },
                        new Setter<ConfigFile, String>() {
                            @Override
                            public void accept(ConfigFile configFile, String strand) {
                                configFile.getGenomeList().get(datasetIndex).getReplicateList().get(replicateIndex).setNormalCodingStrand(strand);
                            }
                        });
        configFileBinder.forField(replicateTab.getNormalTemplate()).asRequired("Please select a graph file (*.gr)")
                .bind(new ValueProvider<ConfigFile, String>() {
                          @Override
                          public String apply(ConfigFile configFile) {
                              return configFile.getGenomeList().get(datasetIndex).getReplicateList().get(replicateIndex).getNormalTemplateStrand();
                          }
                      },
                        new Setter<ConfigFile, String>() {
                            @Override
                            public void accept(ConfigFile configFile, String strand) {
                                configFile.getGenomeList().get(datasetIndex).getReplicateList().get(replicateIndex).setNormalTemplateStrand(strand);
                            }
                        });
    }

    public void addDatasets(int datasetsToAdd) {
        for (int i = 0; i < datasetsToAdd; i++) {
            Genome currentGenome = new Genome();
            //IDs of conditions are set automatically
            if (configFile.isModeConditions()) {
                currentGenome.setAlignmentID("" + (configFile.getNumberOfDatasets() - datasetsToAdd + i));
            }
            //Add as many replicates as the other Genomes have
            for (int j = 0; j < configFile.getNumberOfReplicates(); j++) {
                currentGenome.getReplicateList().add(new Replicate());
            }
            configFile.getGenomeList().add(currentGenome);
        }
    }


    public void removeDatasets(int datasetsToRemove) {
        int oldGenomeListSize = configFile.getGenomeList().size();
        int startIndex = oldGenomeListSize - datasetsToRemove;
        for (int i = startIndex; i < oldGenomeListSize; i++)
            //Remove tail until desired size is reached
            configFile.getGenomeList().remove(configFile.getGenomeList().size() - 1);
    }


    public void addReplicates(int replicatesToAdd) {
        for (int i = 0; i < replicatesToAdd; i++) {
            for (Genome genome : configFile.getGenomeList()) {
                genome.getReplicateList().add(new Replicate());
            }
        }
    }


    public void removeReplicates(int replicatesToRemove) {
        int oldReplicateListSize = configFile.getGenomeList().get(0).getReplicateList().size();
        int startIndex = oldReplicateListSize - replicatesToRemove;
        for (Genome genome : configFile.getGenomeList()) {
            for (int i = startIndex; i < oldReplicateListSize; i++) {
                //Remove tail until desired size is reached
                genome.getReplicateList().remove(genome.getReplicateList().size() - 1);
            }
        }
    }

    public void updateReplicateID(int datasetIndex, int replicateIndex, String id) {
        configFile
                .getGenomeList()
                .get(datasetIndex)
                .getReplicateList()
                .get(replicateIndex)
                .setReplicateID(id);

    }

    public File produceConfigFile() {
        File file = new File("/tmp/tssconfiguration.conf");
        //Check all validators and fields that are set as required
        BinderValidationStatus<ConfigFile> validationStatus = configFileBinder.validate();
        //There will always be 'silent' errors because of non-visible fields.
        //If we're in condition mode, it's 3 errors, in genome mode it's 4 errors
        int silentErrors = configFile.isModeConditions() ? 3 : 4;

        if (validationStatus.getValidationErrors().size() > silentErrors) {
            Notification.show("There are errors (" + (validationStatus.getValidationErrors().size() - silentErrors)
                    + "). Please check the highlighted fields.");
        } else {
            try {
                String configText = configFile.toString();
                FileWriter writer = new FileWriter(file);
                writer.write(configText);
                writer.close();
            } catch (Exception e) {
                //TODO: Log this using Log4J
                e.printStackTrace();
            }
        }
        return file;

    }

    public void applyPresetParameters() {
        switch (preset) {

            case VERY_SPECIFIC:
                configFile.setStepHeight(1);
                configFile.setStepHeightReduction(0.5);
                configFile.setStepFactor(2);
                configFile.setStepFactorReduction(0.5);
                configFile.setEnrichmentFactor(3);
                configFile.setProcessingSiteFactor(1);
                configFile.setStepLength(0);
                configFile.setBaseHeight(0);
                break;
            case MORE_SPECIFIC:
                configFile.setStepHeight(0.5);
                configFile.setStepHeightReduction(0.2);
                configFile.setStepFactor(2);
                configFile.setStepFactorReduction(0.5);
                configFile.setEnrichmentFactor(2);
                configFile.setProcessingSiteFactor(1.2);
                configFile.setStepLength(0);
                configFile.setBaseHeight(0);
                break;
            case DEFAULT:
                configFile.setStepHeight(0.3);
                configFile.setStepHeightReduction(0.2);
                configFile.setStepFactor(2);
                configFile.setStepFactorReduction(0.5);
                configFile.setEnrichmentFactor(2);
                configFile.setProcessingSiteFactor(1.5);
                configFile.setStepLength(0);
                configFile.setBaseHeight(0);
                break;
            case MORE_SENSITIVE:
                configFile.setStepHeight(0.2);
                configFile.setStepHeightReduction(0.15);
                configFile.setStepFactor(1.5);
                configFile.setStepFactorReduction(0.5);
                configFile.setEnrichmentFactor(1.5);
                configFile.setProcessingSiteFactor(2);
                configFile.setStepLength(0);
                configFile.setBaseHeight(0);
                break;
            case VERY_SENSITIVE:
                configFile.setStepHeight(0.1);
                configFile.setStepHeightReduction(0.09);
                configFile.setStepFactor(1);
                configFile.setStepFactorReduction(0.);
                configFile.setEnrichmentFactor(1);
                configFile.setProcessingSiteFactor(3);
                configFile.setStepLength(0);
                configFile.setBaseHeight(0);
                break;
        }
    }

    public void setView(AccordionLayoutMain view) {
        this.view = view;
    }

    public Preset getPreset() {
        return preset;
    }

    public void setPreset(Preset preset) {
        this.preset = preset;
    }

    public int getNumberOfDatasets() {
        return configFile.getNumberOfDatasets();
    }

    public int getNumberOfReplicates() {
        return configFile.getNumberOfReplicates();
    }
}
