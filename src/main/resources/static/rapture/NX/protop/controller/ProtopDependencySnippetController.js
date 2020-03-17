
/*global Ext, NX*/

Ext.define('NX.protop.controller.ProtopDependencySnippetController', {
  extend: 'NX.app.Controller',

  /**
   * @override
   */
  init: function () {
    NX.getApplication().getDependencySnippetController().addDependencySnippetGenerator('protop', this.snippetGenerator);
  },

  snippetGenerator: function (componentModel, assetModel) {
    var group = componentModel.get('group'),
      name = componentModel.get('name'),
      version = componentModel.get('version'),
      dependencyName = '';

    if (group) {
      dependencyName = '@' + group + '/';
    }

    dependencyName = dependencyName + name;

    return [
      {
        displayName: 'Protop',
        description: 'Install proto dependency',
        snippetText: 'protop install ' + dependencyName + '@' + version
      }
    ]
  }
});
