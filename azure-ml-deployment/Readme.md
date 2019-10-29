# Deploy H2O DAI Mojo models to Azure ML Services 


This template is designed to show how to deploy Driverless AI Mojo models to Azure Machine Learning Services. 


### Requisites 

- Driverless AI License 
- Driverless AI Mojo Model
- Driverless AI C++ Runtime Mojo and Datatable package - http://docs.h2o.ai/driverless-ai/latest-stable/docs/userguide/scoring-pipeline-cpp.html
- The [Azure Command Line Intercace]  (https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest)


#### Add the Machine Learning Extension 

```sh 
az extension add -n azure-cli-ml
```

### Configure Deployment

To configure the deployment modify the following files: 

- config.txt
    - worspace_name: Name of the Azure Machine Learning Workspace where to deploy the model
    - resource_group_name: Name of the resource group
    - location: Azure Region where to deploy all the resources. To get a list of valid locations for your account use: `az account list-locations -o table` 
    - model_name: Name that will be used to register the model on Azure
    - description: Short summary about the model
    - model_path: path to the pipeline.mojo file
    - deployment_type: Type of the Deployment (ACI, AKS, Local)
    - aks_cluster: Name of the AKS cluster to be created (only if AKS is selected)
    - dai_mojo_path: path to the C++ Runtime Mojo
    - datatable_path: path to the datatable package
- deploymentconfigACI.json 
- deploymentconfigAKS.json
- deploymentconfigLocal.json

### DAI License 

Must be located in the scoring-pipeline/env directory

### Test Deployment 

In order to test the deployment, the service keys are needed. This deployment script uses secret keys as the default authentication. To get the primary and secondary keys use: 

```
az ml service get-keys -n $service_name -w $workspace_name --resource-group $resource_group_name
```

The test.py script shows how the web service can be consumed. 