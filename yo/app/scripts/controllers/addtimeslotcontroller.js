/**
 * Created by cobos on 05/11/2015.
 */
'use strict';

angular.module('webappApp')
  .controller('AddTimeSlotController', function($scope, $uibModalInstance, TimeSlots, meetingProposalId) {
    $scope.meetingProposalId = meetingProposalId;
    $scope.minDate = new Date();
    $scope.maxDate = new Date();
    $scope.maxDate.setFullYear($scope.maxDate.getFullYear()+5);

    $scope.dateOptions = {
      formatYear: 'yy',
      startingDay: 1
    };

    $scope.hourStep = 1;
    $scope.minuteStep = 15;

    $scope.format = 'dd-MMMM-yyyy';

    $scope.time = new Date();
    $scope.time.setSeconds(0);
    $scope.time.setMilliseconds(0);

    $scope.ok = function () {
      var timeSlot = new TimeSlots();
      timeSlot.dateTime = $scope.time.toJSON();
      timeSlot.meeting = "meetingProposals/"+meetingProposalId;
      timeSlot.$save(timeSlot, function(timeSlotResp){
        $scope.responseTimeSlot = timeSlotResp;
        $uibModalInstance.close($scope.responseTimeSlot);
      });
    };

    $scope.cancel = function () {
      $uibModalInstance.dismiss('cancel');
    };

    $scope.open = function() {
      $scope.status.opened = true;
    };

    $scope.status = {
      opened: false
    };
  });
