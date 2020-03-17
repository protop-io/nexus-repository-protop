
/*global Ext*/

/**
 * Protop plugin configuration
 */
Ext.define('NX.protop.app.PluginConfig', {
  '@aggregate_priority': 100,

  controllers: [
    {
      id: 'NX.protop.controller.ProtopDependencySnippetController',
      active: true
    }
  ]
});
