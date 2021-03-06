'use strict';

/**
 * @ngdoc function
 * @name webappApp.controller:MeetingslistcontrollerCtrl
 * @description
 * # MeetingslistcontrollerCtrl
 * Controller of the webappApp
 */
angular.module('webappApp')
  .controller('MeetingsListController', function($scope, $state, MeetingProposal) {
    MeetingProposal.query()
      .$promise.then(function (meetings) {
        $scope.meetings = meetings._embeddedItems;
      });

    $scope.onClickView = function (meeting){
      if(meeting.key) {
        $state.go('viewMeeting', {'id': meeting.id, 'key': meeting.key});
      }
    };

    $scope.deleteMeeting = function (meeting){
      if(meeting.key) {
        MeetingProposal.delete({id: meeting.id, key: meeting.key}, function () {
          $scope.meetings = $scope.meetings.filter(function (item) {
            return item.id !== meeting.id;
          });
        });
      }
    };
  });
